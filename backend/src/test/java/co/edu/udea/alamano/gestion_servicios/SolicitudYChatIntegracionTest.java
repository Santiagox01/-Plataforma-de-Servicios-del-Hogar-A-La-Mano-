package co.edu.udea.alamano.gestion_servicios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SolicitudYChatIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("PF-01: Solicitud -> aceptación -> chat habilitado (enviar, listar, marcar leído)")
    void pruebaFuncionalSolicitudAceptacionYChat() throws Exception {
        String tokenCliente = iniciarSesion("carlos.cliente@alamano.co", "Cliente123");
        String tokenJuan = iniciarSesion("juan.electricista@alamano.co", "Profesional1");
        Long idServicioElectricidad = obtenerIdServicio(tokenJuan, "Electricidad");

        MockMultipartFile foto1 = new MockMultipartFile("fotos", "tablero1.png", "image/png", new byte[]{1, 2, 3});
        MockMultipartFile foto2 = new MockMultipartFile("fotos", "tablero2.jpg", "image/jpeg", new byte[]{4, 5, 6});

        MvcResult resSolicitud = mockMvc.perform(multipart("/api/solicitudes")
                        .file(foto1)
                        .file(foto2)
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("idServicio", String.valueOf(idServicioElectricidad))
                        .param("descripcion", "Breaker principal se dispara al encender la ducha.")
                        .param("esUrgente", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoSolicitud").value("PENDIENTE"))
                .andExpect(jsonPath("$.urlsFotos.length()").value(2))
                .andExpect(jsonPath("$.chatHabilitado").value(false))
                .andReturn();

        Long idSolicitud = objectMapper.readTree(resSolicitud.getResponse().getContentAsString()).get("idSolicitud").asLong();

        // Chat bloqueado mientras este PENDIENTE
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/chat/mensajes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\": \"¿Ya vienes?\"}"))
                .andExpect(status().isBadRequest());

        // Profesional acepta -> chat habilitado
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/aceptar")
                        .header("Authorization", "Bearer " + tokenJuan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSolicitud").value("ACEPTADA"))
                .andExpect(jsonPath("$.chatHabilitado").value(true));

        // Cliente envia mensaje y profesional lo marca como LEIDO
        MvcResult msgRes = mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/chat/mensajes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\": \"Estoy en el apartamento 402.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoMensaje").value("ENVIADO"))
                .andReturn();
        Long idMensaje = objectMapper.readTree(msgRes.getResponse().getContentAsString()).get("idMensaje").asLong();

        mockMvc.perform(get("/api/solicitudes/" + idSolicitud + "/chat/mensajes")
                        .header("Authorization", "Bearer " + tokenJuan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contenido").value("Estoy en el apartamento 402."));

        mockMvc.perform(patch("/api/solicitudes/" + idSolicitud + "/chat/mensajes/" + idMensaje + "/leer")
                        .header("Authorization", "Bearer " + tokenJuan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoMensaje").value("LEIDO"));
    }

    @Test
    @DisplayName("HU08 y HU09: Configurar tarifa y cotizar precio estimado en vivo con recargo y tope")
    void configurarTarifaYCotizarEnVivo() throws Exception {
        String tokenJuan = iniciarSesion("juan.electricista@alamano.co", "Profesional1");
        String tokenCliente = iniciarSesion("carlos.cliente@alamano.co", "Cliente123");
        Long idCatElectricidad = obtenerIdCategoria("Electricidad");
        Long idServicioElectricidad = obtenerIdServicio(tokenJuan, "Electricidad");

        // Rango invalido -> 400
        mockMvc.perform(put("/api/profesionales/me/tarifas")
                        .header("Authorization", "Bearer " + tokenJuan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\":" + idCatElectricidad + ",\"tarifaBase\":50000,\"precioMin\":60000,\"precioMax\":40000}"))
                .andExpect(status().isBadRequest());

        // Configurar min=40000, max=60000, base=50000
        mockMvc.perform(put("/api/profesionales/me/tarifas")
                        .header("Authorization", "Bearer " + tokenJuan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\":" + idCatElectricidad + ",\"tarifaBase\":50000,\"precioMin\":40000,\"precioMax\":60000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaBase").value(50000));

        // Cotizacion urgente en vivo -> acotada a 60000
        mockMvc.perform(get("/api/cotizaciones/estimar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("servicioId", String.valueOf(idServicioElectricidad))
                        .param("esUrgente", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioEstimado").value(60000))
                .andExpect(jsonPath("$.recargoUrgenciaAplicado").value(true))
                .andExpect(jsonPath("$.acotadoAlMaximo").value(true));
    }

    @Test
    @DisplayName("HU07 y HU10: Doble aceptación concurrente (una gana 200 y otra 409) y transición inválida")
    void dobleAceptacionConcurrenteYTransicionInvalida() throws Exception {
        String tokenCliente = iniciarSesion("laura.cliente@alamano.co", "Cliente123");
        String tokenMaria = iniciarSesion("maria.plomera@alamano.co", "Profesional1");
        Long idServicioPlomeria = obtenerIdServicio(tokenMaria, "Plomería");

        MvcResult creada = mockMvc.perform(multipart("/api/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("idServicio", String.valueOf(idServicioPlomeria))
                        .param("descripcion", "Cambio de llave de paso")
                        .param("esUrgente", "false"))
                .andExpect(status().isCreated())
                .andReturn();
        Long idSolicitud = objectMapper.readTree(creada.getResponse().getContentAsString()).get("idSolicitud").asLong();

        // Transicion invalida: registrar precio final antes de aceptar
        mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/precio-final")
                        .header("Authorization", "Bearer " + tokenMaria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioFinal\": 48000}"))
                .andExpect(status().isBadRequest());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch barrera = new CountDownLatch(1);
        Callable<Integer> tareaAceptar = () -> {
            barrera.await();
            return mockMvc.perform(post("/api/solicitudes/" + idSolicitud + "/aceptar")
                            .header("Authorization", "Bearer " + tokenMaria))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        Future<Integer> f1 = pool.submit(tareaAceptar);
        Future<Integer> f2 = pool.submit(tareaAceptar);
        barrera.countDown();

        List<Integer> estados = new ArrayList<>();
        estados.add(f1.get());
        estados.add(f2.get());
        pool.shutdown();

        assertThat(estados).containsExactlyInAnyOrder(200, 409);
    }

    @Test
    @DisplayName("HU07, HU10 y HU12: Rechazo con sugerencias, disputa por precio final, confirmación y reseña única")
    void rechazoDisputaConfirmacionYResena() throws Exception {
        String tokenCliente = iniciarSesion("carlos.cliente@alamano.co", "Cliente123");
        String tokenMaria = iniciarSesion("maria.plomera@alamano.co", "Profesional1");
        String tokenAdmin = iniciarSesion("admin@alamano.co", "Admin1234");
        String tokenJuan = iniciarSesion("juan.electricista@alamano.co", "Profesional1");

        // 1. Registrar otro plomero verificado y disponible para verificar sugerencias tras rechazo
        Long idCatPlomeria = obtenerIdCategoria("Plomería");
        MockMultipartFile docPdf = new MockMultipartFile("documentoIdentidad", "cc.pdf", "application/pdf", "%PDF-1.4".getBytes());
        MvcResult regPro = mockMvc.perform(multipart("/api/auth/registro/profesional")
                        .file(docPdf)
                        .param("nombre", "Luis Plomero Alterno")
                        .param("correo", "luis.plomero@alamano.co")
                        .param("celular", "3191112233")
                        .param("contrasena", "Profesional1")
                        .param("ubicacion", "El Poblado")
                        .param("latitud", "6.2090")
                        .param("longitud", "-75.5700")
                        .param("categoriasIds", String.valueOf(idCatPlomeria))
                        .param("aceptaTerminos", "true"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode proJson = objectMapper.readTree(regPro.getResponse().getContentAsString());
        Long idLuis = proJson.get("usuario").get("idUsuario").asLong();
        String tokenLuis = proJson.get("token").asText();

        mockMvc.perform(post("/api/admin/profesionales/" + idLuis + "/aprobar")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/profesionales/me/disponibilidad")
                        .header("Authorization", "Bearer " + tokenLuis)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponible\": true}"))
                .andExpect(status().isOk());

        // 2. Maria rechaza una solicitud y el sistema sugiere a Luis
        Long idServicioMaria = obtenerIdServicio(tokenMaria, "Plomería");
        MvcResult solRechazoRes = mockMvc.perform(multipart("/api/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("idServicio", String.valueOf(idServicioMaria))
                        .param("descripcion", "Desagüe tapado")
                        .param("esUrgente", "false"))
                .andExpect(status().isCreated())
                .andReturn();
        Long idSolRechazo = objectMapper.readTree(solRechazoRes.getResponse().getContentAsString()).get("idSolicitud").asLong();

        mockMvc.perform(post("/api/solicitudes/" + idSolRechazo + "/rechazar")
                        .header("Authorization", "Bearer " + tokenMaria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Estoy atendiendo otra emergencia.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitud.estadoSolicitud").value("RECHAZADA"))
                .andExpect(jsonPath("$.profesionalesSugeridos[0].nombre").value("Luis Plomero Alterno"));

        // 3. Flujo de precio final rechazado -> EN_DISPUTA + notificacion al admin
        Long idServicioGasJuan = obtenerIdServicio(tokenJuan, "Gas");
        MvcResult solDisputaRes = mockMvc.perform(multipart("/api/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("idServicio", String.valueOf(idServicioGasJuan))
                        .param("descripcion", "Cambio de manguera de gas")
                        .param("esUrgente", "false"))
                .andExpect(status().isCreated())
                .andReturn();
        Long idSolDisputa = objectMapper.readTree(solDisputaRes.getResponse().getContentAsString()).get("idSolicitud").asLong();

        mockMvc.perform(post("/api/solicitudes/" + idSolDisputa + "/aceptar")
                        .header("Authorization", "Bearer " + tokenJuan))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + idSolDisputa + "/precio-final")
                        .header("Authorization", "Bearer " + tokenJuan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioFinal\": 95000}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + idSolDisputa + "/rechazar-precio-final")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"No se acordó ese sobrecosto.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSolicitud").value("EN_DISPUTA"));

        mockMvc.perform(get("/api/notificaciones")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("DISPUTA_SOPORTE"));

        // 4. Flujo de confirmacion + resena unica + recalculo real de promedio en perfil (HU06/HU12)
        MvcResult solOkRes = mockMvc.perform(multipart("/api/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("idServicio", String.valueOf(idServicioGasJuan))
                        .param("descripcion", "Calibración de quemadores")
                        .param("esUrgente", "false"))
                .andExpect(status().isCreated())
                .andReturn();
        Long idSolOk = objectMapper.readTree(solOkRes.getResponse().getContentAsString()).get("idSolicitud").asLong();

        mockMvc.perform(post("/api/solicitudes/" + idSolOk + "/aceptar")
                        .header("Authorization", "Bearer " + tokenJuan))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + idSolOk + "/precio-final")
                        .header("Authorization", "Bearer " + tokenJuan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioFinal\": 55000}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + idSolOk + "/confirmar-precio-final")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSolicitud").value("FINALIZADA"))
                .andExpect(jsonPath("$.precioFinalConfirmado").value(true));

        // Juan ya tenia 1 resena de 5 en V4; con esta de 3, promedio debe ser 4.0
        mockMvc.perform(post("/api/solicitudes/" + idSolOk + "/resena")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estrellas\": 3, \"comentario\": \"Buen trabajo pero tardó un poco.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nuevoPromedioProfesional").value(4.0))
                .andExpect(jsonPath("$.totalResenasProfesional").value(2));

        // Segunda resena sobre misma solicitud -> 409 Conflict
        mockMvc.perform(post("/api/solicitudes/" + idSolOk + "/resena")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estrellas\": 5, \"comentario\": \"Duplicada\"}"))
                .andExpect(status().isConflict());

        // HU06 muestra reputacion real actualizada
        Long idJuan = obtenerMiId(tokenJuan);
        mockMvc.perform(get("/api/profesionales/" + idJuan)
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacionPromedio").value(4.0))
                .andExpect(jsonPath("$.totalResenas").value(2))
                .andExpect(jsonPath("$.ultimosComentarios[0].comentario").value("Buen trabajo pero tardó un poco."));
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

    private Long obtenerMiId(String token) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("idUsuario").asLong();
    }

    private Long obtenerIdCategoria(String nombre) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/categorias")).andExpect(status().isOk()).andReturn();
        for (JsonNode n : objectMapper.readTree(res.getResponse().getContentAsString())) {
            if (nombre.equals(n.get("nombre").asText())) {
                return n.get("idCategoria").asLong();
            }
        }
        throw new IllegalStateException("Categoría no encontrada: " + nombre);
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
