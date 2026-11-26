package co.edu.udea.alamano.moderacion_reportes;

import co.edu.udea.alamano.comun.auditoria.AuditoriaAdministrativaRepositorio;
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

import java.time.YearMonth;
import java.util.Map;

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
class ModeracionYSoporteIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditoriaAdministrativaRepositorio auditoriaRepositorio;

    @Test
    @DisplayName("HU15/HU16/HU17: Reporte exige solicitud, reincidencia prioriza cola, anonimato, resolución inmutable, reembolso y sanción polimórfica")
    void flujoCompletoReportesModeracionYSanciones() throws Exception {
        String tokenAdmin = iniciarSesion("admin@alamano.co", "Admin1234");
        String correoCliente = "cliente.moderacion@alamano.co";
        String tokenCliente = registrarCliente(correoCliente, "Cliente Moderación");

        String correoPro = "pro.moderacion@alamano.co";
        ProfesionalTestData pro = registrarYAprobarProfesional(tokenAdmin, correoPro, "Profesional Moderación");

        // 1. Reporte sin solicitud asociada -> 400 Bad Request
        mockMvc.perform(post("/api/reportes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"MAL_COMPORTAMIENTO\", \"descripcion\": \"Sin solicitud\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("solicitud existente")));

        // 2. Crear dos solicitudes reales y pagar la segunda para probar tambien reembolso por moderacion
        Long sol1 = crearSolicitud(tokenCliente, pro.idServicio(), "Reparación toma eléctrica 1");
        mockMvc.perform(post("/api/solicitudes/" + sol1 + "/aceptar")
                        .header("Authorization", "Bearer " + pro.token()))
                .andExpect(status().isOk());

        Long sol2 = crearSolicitud(tokenCliente, pro.idServicio(), "Reparación toma eléctrica 2");
        mockMvc.perform(post("/api/solicitudes/" + sol2 + "/aceptar")
                        .header("Authorization", "Bearer " + pro.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + sol2 + "/precio-final")
                        .header("Authorization", "Bearer " + pro.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioFinal\": 50000}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + sol2 + "/confirmar-precio-final")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk());

        YearMonth futuro = YearMonth.now().plusYears(2);
        MvcResult cardRes = mockMvc.perform(post("/api/metodos-pago")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "numeroTarjeta", "4242424242424242",
                                "cvv", "777",
                                "mesVencimiento", futuro.getMonthValue(),
                                "anioVencimiento", futuro.getYear(),
                                "tipo", "TARJETA_CREDITO",
                                "titular", "Cliente Moderación"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        Long idMetodo = objectMapper.readTree(cardRes.getResponse().getContentAsString()).get("idMetodoPago").asLong();

        mockMvc.perform(post("/api/solicitudes/" + sol2 + "/pagar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMetodoPago\": " + idMetodo + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPago").value("RETENIDO"));

        // 3. Primer reporte (no prioritario) y segundo reporte contra el mismo profesional (reincidencia -> esPrioritario = true)
        MvcResult rep1Res = mockMvc.perform(post("/api/reportes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSolicitud\": " + sol1 + ", \"motivo\": \"NO_SE_PRESENTO\", \"descripcion\": \"Llegó tarde sin avisar.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esPrioritario").value(false))
                .andReturn();
        Long idRep1 = objectMapper.readTree(rep1Res.getResponse().getContentAsString()).get("idReporte").asLong();

        MvcResult rep2Res = mockMvc.perform(post("/api/reportes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSolicitud\": " + sol2 + ", \"motivo\": \"MALA_CALIDAD\", \"descripcion\": \"Dejó el tablero haciendo falso contacto.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esPrioritario").value(true))
                .andReturn();
        Long idRep2 = objectMapper.readTree(rep2Res.getResponse().getContentAsString()).get("idReporte").asLong();

        // Cola ordenada con prioritario primero
        mockMvc.perform(get("/api/admin/reportes")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idReporte").value(idRep2))
                .andExpect(jsonPath("$[0].esPrioritario").value(true))
                .andExpect(jsonPath("$[0].reportesPreviosReportado").value(1))
                .andExpect(jsonPath("$[0].tienePagoRetenido").value(true));

        // 4. El reportado NO ve quien lo reporto
        MvcResult recibidosRes = mockMvc.perform(get("/api/reportes/recibidos")
                        .header("Authorization", "Bearer " + pro.token()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(recibidosRes.getResponse().getContentAsString())
                .doesNotContain("Cliente Moderación")
                .doesNotContain("idReportante")
                .doesNotContain("nombreReportante");

        // 5. Descartar primer reporte como "Sin merito" -> notifica a ambas partes e impide editar luego (inmutable)
        mockMvc.perform(post("/api/admin/reportes/" + idRep1 + "/resolver")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\": \"DESCARTAR\", \"justificacion\": \"Sin mérito por falta de soporte fotográfico.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReporte").value("DESCARTADO"));

        mockMvc.perform(get("/api/notificaciones").header("Authorization", "Bearer " + pro.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("REPORTE_DESCARTADO"));
        mockMvc.perform(get("/api/notificaciones").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("REPORTE_DESCARTADO"));

        // Reintento sobre reporte ya resuelto -> 409 Conflict (inmutable)
        mockMvc.perform(post("/api/admin/reportes/" + idRep1 + "/resolver")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\": \"ADVERTENCIA\", \"justificacion\": \"Intento de cambio\"}"))
                .andExpect(status().isConflict());

        // 6. Resolver segundo reporte con SUSPENSION_TEMPORAL + REEMBOLSAR pago retenido
        mockMvc.perform(post("/api/admin/reportes/" + idRep2 + "/resolver")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accion": "SUSPENSION_TEMPORAL",
                                  "diasSuspension": 5,
                                  "decisionPago": "REEMBOLSAR",
                                  "justificacion": "Reincidencia verificada en instalación eléctrica defectuosa."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReporte").value("RESUELTO"))
                .andExpect(jsonPath("$.accionTomada").value("SUSPENSION_TEMPORAL"));

        // Auditoria registrada
        assertThat(auditoriaRepositorio.findByEntidadAfectadaAndIdEntidadOrderByFechaAccionDesc("REPORTE", idRep2))
                .anyMatch(a -> "RESOLVER_REPORTE_SUSPENSION_TEMPORAL".equals(a.getAccion()));

        // Pago quedo REEMBOLSADO
        mockMvc.perform(get("/api/solicitudes/" + sol2 + "/pago")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("REEMBOLSADO"));

        // Profesional suspendido NO puede loguearse (403 con aviso) ni operar ni aparecer en busqueda
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("correo", correoPro, "contrasena", "Profesional1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("suspendida")));

        mockMvc.perform(get("/api/profesionales/me/servicios")
                        .header("Authorization", "Bearer " + pro.token()))
                .andExpect(status().isUnauthorized());

        MvcResult busquedaRes = mockMvc.perform(get("/api/profesionales/buscar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("categoriaId", String.valueOf(pro.idCategoria()))
                        .param("latitud", "6.2090")
                        .param("longitud", "-75.5700")
                        .param("radioKm", "10"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(busquedaRes.getResponse().getContentAsString()).doesNotContain("Profesional Moderación");

        // 7. Sancion polimorfica sobre un cliente fresco (profesional reporta cliente -> DESACTIVACION)
        String correoClienteSancion = "cliente.desactivar@alamano.co";
        String tokenClienteSancion = registrarCliente(correoClienteSancion, "Cliente Sancionado");
        ProfesionalTestData pro2 = registrarYAprobarProfesional(tokenAdmin, "pro.activo2@alamano.co", "Pro Test Activo");

        Long sol3 = crearSolicitud(tokenClienteSancion, pro2.idServicio(), "Servicio para validar sanción a cliente");
        MvcResult repClienteRes = mockMvc.perform(post("/api/reportes")
                        .header("Authorization", "Bearer " + pro2.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSolicitud\": " + sol3 + ", \"motivo\": \"MALTRATO\", \"descripcion\": \"Trato agresivo.\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long idRepCliente = objectMapper.readTree(repClienteRes.getResponse().getContentAsString()).get("idReporte").asLong();

        mockMvc.perform(post("/api/admin/reportes/" + idRepCliente + "/resolver")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accion\": \"DESACTIVACION\", \"justificacion\": \"Maltrato comprobado.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoReporte").value("RESUELTO"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("correo", correoClienteSancion, "contrasena", "Cliente123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("desactivada")));
    }

    @Test
    @DisplayName("HU18: Ticket de soporte con número único, respuesta administrativa y enlace automático desde EN_DISPUTA")
    void flujoSoporteYDisputaEnlazada() throws Exception {
        String tokenCliente = iniciarSesion("carlos.cliente@alamano.co", "Cliente123");
        String tokenAdmin = iniciarSesion("admin@alamano.co", "Admin1234");
        String tokenJuan = iniciarSesion("juan.electricista@alamano.co", "Profesional1");
        Long idServicioJuan = obtenerIdServicio(tokenJuan, "Electricidad");

        // 1. Usuario crea caso de soporte con confirmacion inmediata
        MvcResult casoRes = mockMvc.perform(post("/api/soporte/casos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "asunto": "Duda sobre actualización de teléfono",
                                  "descripcion": "Necesito saber cómo cambiar mi número celular registrado."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCaso").value(org.hamcrest.Matchers.startsWith("SUP-")))
                .andExpect(jsonPath("$.estadoCaso").value("ABIERTO"))
                .andReturn();
        Long idCaso = objectMapper.readTree(casoRes.getResponse().getContentAsString()).get("idCaso").asLong();

        // 2. Administrador responde al caso
        mockMvc.perform(post("/api/admin/soporte/casos/" + idCaso + "/responder")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"respuesta\": \"Tu solicitud fue atendida por soporte.\", \"cerrar\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCaso").value("CERRADO"))
                .andExpect(jsonPath("$.respuestaAdmin").value("Tu solicitud fue atendida por soporte."));

        // Usuario ve respuesta en su lista
        mockMvc.perform(get("/api/soporte/mis-casos")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoCaso").value("CERRADO"))
                .andExpect(jsonPath("$[0].respuestaAdmin").value("Tu solicitud fue atendida por soporte."));

        // 3. Disputa de precio final crea automaticamente CasoSoporte visible para el admin
        Long idSolDisputa = crearSolicitud(tokenCliente, idServicioJuan, "Cambio de taco eléctrico");
        mockMvc.perform(post("/api/solicitudes/" + idSolDisputa + "/aceptar")
                        .header("Authorization", "Bearer " + tokenJuan))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + idSolDisputa + "/precio-final")
                        .header("Authorization", "Bearer " + tokenJuan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioFinal\": 110000}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/solicitudes/" + idSolDisputa + "/rechazar-precio-final")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Cobro muy superior al estimado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoSolicitud").value("EN_DISPUTA"));

        MvcResult bandejaAdmin = mockMvc.perform(get("/api/admin/soporte/casos")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(bandejaAdmin.getResponse().getContentAsString())
                .contains("Disputa por precio final — Solicitud #" + idSolDisputa);
    }

    private ProfesionalTestData registrarYAprobarProfesional(String tokenAdmin, String correo, String nombre) throws Exception {
        Long idCat = obtenerIdCategoria("Electricidad");
        MockMultipartFile docPdf = new MockMultipartFile("documentoIdentidad", "cc.pdf", "application/pdf", "%PDF-1.4".getBytes());
        MvcResult reg = mockMvc.perform(multipart("/api/auth/registro/profesional")
                        .file(docPdf)
                        .param("nombre", nombre)
                        .param("correo", correo)
                        .param("celular", "317" + String.valueOf(Math.abs(correo.hashCode()) % 10000000 + 1000000).substring(0, 7))
                        .param("contrasena", "Profesional1")
                        .param("ubicacion", "El Poblado")
                        .param("latitud", "6.2090")
                        .param("longitud", "-75.5700")
                        .param("categoriasIds", String.valueOf(idCat))
                        .param("aceptaTerminos", "true"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(reg.getResponse().getContentAsString());
        Long idPro = json.get("usuario").get("idUsuario").asLong();
        String token = json.get("token").asText();

        mockMvc.perform(post("/api/admin/profesionales/" + idPro + "/aprobar")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/profesionales/me/disponibilidad")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponible\": true}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/profesionales/me/tarifas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCategoria\": " + idCat + ", \"tarifaBase\": 50000, \"precioMin\": 40000, \"precioMax\": 80000}"))
                .andExpect(status().isOk());

        Long idServicio = obtenerIdServicio(token, "Electricidad");
        return new ProfesionalTestData(idPro, token, idCat, idServicio);
    }

    private String registrarCliente(String correo, String nombre) throws Exception {
        String celular = "306" + String.valueOf(Math.abs(correo.hashCode()) % 10000000 + 1000000).substring(0, 7);
        String payload = objectMapper.writeValueAsString(Map.of(
                "nombre", nombre,
                "correo", correo,
                "celular", celular,
                "contrasena", "Cliente123",
                "ubicacion", "El Poblado, Medellín",
                "latitud", 6.2088,
                "longitud", -75.5695,
                "aceptaTerminos", true
        ));
        MvcResult res = mockMvc.perform(post("/api/auth/registro/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private Long crearSolicitud(String tokenCliente, Long idServicio, String desc) throws Exception {
        MvcResult res = mockMvc.perform(multipart("/api/solicitudes")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("idServicio", String.valueOf(idServicio))
                        .param("descripcion", desc)
                        .param("esUrgente", "false"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("idSolicitud").asLong();
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

    private record ProfesionalTestData(Long idUsuario, String token, Long idCategoria, Long idServicio) {
    }
}
