package co.edu.udea.alamano.pagos;

import co.edu.udea.alamano.pagos.dominio.MetodoPago;
import co.edu.udea.alamano.pagos.infraestructura.MetodoPagoRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PagosIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MetodoPagoRepositorio metodoPagoRepositorio;

    @Test
    @DisplayName("PF-02: Confirmar precio final, pago exitoso en custodia (RETENIDO -> PAGADA), comprobante y liberación")
    void pruebaFuncionalConfirmacionYPagoExitoso() throws Exception {
        String tokenCliente = iniciarSesion("carlos.cliente@alamano.co", "Cliente123");
        String tokenJuan = iniciarSesion("juan.electricista@alamano.co", "Profesional1");
        Long idServicioGas = obtenerIdServicio(tokenJuan, "Gas");

        // Registrar tarjeta aprobada para Carlos
        YearMonth futuro = YearMonth.now().plusYears(2);
        MvcResult regCard = mockMvc.perform(post("/api/metodos-pago")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarjetaJson("4242424242424242", "888", futuro, "TARJETA_CREDITO", "Carlos Ramírez")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ultimosDigitos").value("4242"))
                .andReturn();
        assertThat(regCard.getResponse().getContentAsString()).doesNotContain("4242424242424242").doesNotContain("888");
        Long idMetodoValido = objectMapper.readTree(regCard.getResponse().getContentAsString()).get("idMetodoPago").asLong();

        MetodoPago metodoPersistido = metodoPagoRepositorio.findById(idMetodoValido).orElseThrow();
        assertThat(metodoPersistido.getToken()).startsWith("tok_sim_ok_");
        assertThat(metodoPersistido.getUltimosDigitos()).isEqualTo("4242");

        // Flujo de solicitud hasta FINALIZADA con precio final 60.000
        Long idSolicitud = crearYAceptarSolicitud(tokenCliente, tokenJuan, idServicioGas, "Revisión de acometida");
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/precio-final")
                        .header("Authorization", "Bearer " + tokenJuan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioFinal\": 60000}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/confirmar-precio-final")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSolicitud").value("FINALIZADA"));

        // Pago exitoso: total = 60.000 + 3.000 garantia = 63.000
        MvcResult pagoRes = mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/pagar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMetodoPago\": " + idMetodoValido + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPago").value("RETENIDO"))
                .andExpect(jsonPath("$.montoServicio").value(60000))
                .andExpect(jsonPath("$.tasaGarantia").value(3000))
                .andExpect(jsonPath("$.cantidad").value(63000))
                .andExpect(jsonPath("$.comprobante.numeroComprobante").isNotEmpty())
                .andReturn();
        Long idPago = objectMapper.readTree(pagoRes.getResponse().getContentAsString()).get("idPago").asLong();
        String refIdempotencia = objectMapper.readTree(pagoRes.getResponse().getContentAsString())
                .get("referenciaIdempotencia").asText();

        // Doble clic idempotente: no cobra dos veces y retorna el mismo pago
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/pagar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMetodoPago\": " + idMetodoValido + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotente").value(true))
                .andExpect(jsonPath("$.idPago").value(idPago))
                .andExpect(jsonPath("$.referenciaIdempotencia").value(refIdempotencia));

        // Solicitud queda PAGADA y ambos (cliente y profesional) acceden al comprobante
        mockMvc.perform(get("/api/solicitudes/" + idSolicitud)
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSolicitud").value("PAGADA"))
                .andExpect(jsonPath("$.puedeLiberarCustodia").value(true));

        mockMvc.perform(get("/api/solicitudes/" + idSolicitud + "/pago")
                        .header("Authorization", "Bearer " + tokenJuan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprobante.totalPagado").value(63000));

        // Liberacion de custodia por el cliente
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/pago/liberar")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("LIBERADO"));

        mockMvc.perform(get("/api/notificaciones")
                        .header("Authorization", "Bearer " + tokenJuan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("PAGO_LIBERADO"));
    }

    @Test
    @DisplayName("HU13/HU14: Tarjeta rechazada no se guarda, no se paga no FINALIZADA, rechazo por fondos y reintento exitoso")
    void rechazoRegistroFondosInsuficientesYReintento() throws Exception {
        String tokenCliente = registrarClienteFresco("cliente.pagos@alamano.co");
        String tokenMaria = iniciarSesion("maria.plomera@alamano.co", "Profesional1");
        Long idServicioCerrajeria = obtenerIdServicio(tokenMaria, "Cerrajería");
        YearMonth futuro = YearMonth.now().plusYears(2);

        // 1. Tarjeta terminada en 0001 rechazada en registro -> 400 y NO se guarda
        mockMvc.perform(post("/api/metodos-pago")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarjetaJson("4111111111110001", "111", futuro, "TARJETA_CREDITO", "Cliente Pagos")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("rechazada")));

        // Tarjeta vencida rechazada -> 400 y NO se guarda
        YearMonth vencida = YearMonth.now().minusMonths(2);
        mockMvc.perform(post("/api/metodos-pago")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarjetaJson("4242424242424242", "111", vencida, "TARJETA_CREDITO", "Cliente Pagos")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/metodos-pago")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 2. Registrar tarjeta sin fondos (...0002) y tarjeta valida
        MvcResult regSinFondos = mockMvc.perform(post("/api/metodos-pago")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarjetaJson("5555555555550002", "222", futuro, "TARJETA_DEBITO", "Cliente Pagos")))
                .andExpect(status().isCreated())
                .andReturn();
        Long idTarjetaSinFondos = objectMapper.readTree(regSinFondos.getResponse().getContentAsString())
                .get("idMetodoPago").asLong();

        MvcResult regValida = mockMvc.perform(post("/api/metodos-pago")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarjetaJson("5555555555554444", "333", futuro, "TARJETA_CREDITO", "Cliente Pagos")))
                .andExpect(status().isCreated())
                .andReturn();
        Long idTarjetaValida = objectMapper.readTree(regValida.getResponse().getContentAsString())
                .get("idMetodoPago").asLong();

        // 3. No se puede pagar solicitud que no esta FINALIZADA
        Long idSolicitud = crearYAceptarSolicitud(tokenCliente, tokenMaria, idServicioCerrajeria, "Cambio chapa");
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/pagar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMetodoPago\": " + idTarjetaValida + "}"))
                .andExpect(status().isBadRequest());

        // Llevar a FINALIZADA
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/precio-final")
                        .header("Authorization", "Bearer " + tokenMaria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioFinal\": 42000}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/confirmar-precio-final")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk());

        // 4. Intento con tarjeta 0002 -> Pago RECHAZADO, solicitud sigue FINALIZADA y notifica cliente
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/pagar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMetodoPago\": " + idTarjetaSinFondos + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivoRechazo").value(org.hamcrest.Matchers.containsString("Fondos insuficientes")));

        mockMvc.perform(get("/api/solicitudes/" + idSolicitud)
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSolicitud").value("FINALIZADA"))
                .andExpect(jsonPath("$.puedePagar").value(true));

        // 5. Reintento con tarjeta valida -> RETENIDO y solicitud PAGADA
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/pagar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMetodoPago\": " + idTarjetaValida + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPago").value("RETENIDO"))
                .andExpect(jsonPath("$.cantidad").value(45000));

        mockMvc.perform(get("/api/solicitudes/" + idSolicitud)
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSolicitud").value("PAGADA"));
    }

    private String tarjetaJson(String numero, String cvv, YearMonth ym, String tipo, String titular) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "numeroTarjeta", numero,
                "cvv", cvv,
                "mesVencimiento", ym.getMonthValue(),
                "anioVencimiento", ym.getYear(),
                "tipo", tipo,
                "titular", titular
        ));
    }

    private Long crearYAceptarSolicitud(String tokenCliente, String tokenPro, Long idServicio, String desc) throws Exception {
        MvcResult creada = mockMvc.perform(multipart("/api/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("idServicio", String.valueOf(idServicio))
                        .param("descripcion", desc)
                        .param("esUrgente", "false"))
                .andExpect(status().isCreated())
                .andReturn();
        Long idSolicitud = objectMapper.readTree(creada.getResponse().getContentAsString()).get("idSolicitud").asLong();

        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/aceptar")
                        .header("Authorization", "Bearer " + tokenPro))
                .andExpect(status().isOk());
        return idSolicitud;
    }

    private String registrarClienteFresco(String correo) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "nombre", "Cliente Pagos",
                "correo", correo,
                "celular", "3058887766",
                "contrasena", "Cliente123",
                "ubicacion", "Laureles, Medellín",
                "latitud", 6.2447,
                "longitud", -75.5950,
                "aceptaTerminos", true
        ));
        MvcResult res = mockMvc.perform(post("/api/auth/registro/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String iniciarSesion(String correo, String contrasena) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("correo", correo, "contrasena", contrasena));
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private Long obtenerIdServicio(String tokenProfesional, String nombreCategoria) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/profesionales/me/servicios")
                        .header("Authorization", "Bearer " + tokenProfesional))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode n : objectMapper.readTree(res.getResponse().getContentAsString())) {
            if (nombreCategoria.equals(n.get("nombreCategoria").asText())) {
                return n.get("idServicio").asLong();
            }
        }
        throw new IllegalStateException("Servicio no encontrado: " + nombreCategoria);
    }
}
