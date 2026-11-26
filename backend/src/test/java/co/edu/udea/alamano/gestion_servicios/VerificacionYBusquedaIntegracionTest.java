package co.edu.udea.alamano.gestion_servicios;

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

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VerificacionYBusquedaIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditoriaAdministrativaRepositorio auditoriaRepositorio;

    @Test
    @DisplayName("PF-03: Profesional pendiente no aparece en búsqueda; tras aprobación y disponibilidad sí aparece")
    void pruebaFuncionalBusquedaProfesionalesVerificados() throws Exception {
        String tokenCliente = iniciarSesion("carlos.cliente@alamano.co", "Cliente123");
        String tokenAdmin = iniciarSesion("admin@alamano.co", "Admin1234");
        String tokenPedro = iniciarSesion("pedro.cerrajero@alamano.co", "Profesional1");

        // Obtener id de categoria Cerrajeria
        Long idCerrajeria = obtenerIdCategoria("Cerrajería");
        Long idPedro = obtenerMiId(tokenPedro);

        // 1. Cliente no puede usar endpoints administrativos
        mockMvc.perform(get("/api/admin/profesionales/pendientes")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());

        // 2. Pedro (pendiente) NO aparece en busqueda en Envigado (radio 5 km)
        mockMvc.perform(get("/api/profesionales/buscar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("categoriaId", String.valueOf(idCerrajeria))
                        .param("latitud", "6.1719")
                        .param("longitud", "-75.5863")
                        .param("radioKm", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResultados").value(0))
                .andExpect(jsonPath("$.mensajeSugerencia").isNotEmpty());

        // 3. Admin ve a Pedro en pendientes y consulta su documento mediante URL firmada inline
        mockMvc.perform(get("/api/admin/profesionales/pendientes")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("pedro.cerrajero@alamano.co"));

        MvcResult urlRes = mockMvc.perform(get("/api/admin/profesionales/" + idPedro + "/documento-url")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        String urlFirmada = objectMapper.readTree(urlRes.getResponse().getContentAsString())
                .get("urlDocumentoIdentidad").asText();
        String rutaFirmada = URI.create(urlFirmada).getPath();

        mockMvc.perform(get(rutaFirmada))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));

        // 4. Pedro intenta ponerse disponible antes de ser verificado -> rechazado
        mockMvc.perform(patch("/api/profesionales/me/disponibilidad")
                        .header("Authorization", "Bearer " + tokenPedro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponible\": true}"))
                .andExpect(status().isBadRequest());

        // 5. Admin aprueba a Pedro y se registra auditoria
        mockMvc.perform(post("/api/admin/profesionales/" + idPedro + "/aprobar")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVerificacion").value("VERIFICADO"));

        assertThat(auditoriaRepositorio.findByEntidadAfectadaAndIdEntidadOrderByFechaAccionDesc("PROFESIONAL", idPedro))
                .anyMatch(a -> "APROBAR_PROFESIONAL".equals(a.getAccion()));

        // 6. Pedro activa su disponibilidad y ahora SI aparece en la busqueda
        mockMvc.perform(patch("/api/profesionales/me/disponibilidad")
                        .header("Authorization", "Bearer " + tokenPedro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponible\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));

        mockMvc.perform(get("/api/profesionales/buscar")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .param("categoriaId", String.valueOf(idCerrajeria))
                        .param("latitud", "6.1719")
                        .param("longitud", "-75.5863")
                        .param("radioKm", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResultados").value(1))
                .andExpect(jsonPath("$.resultados[0].nombre").value("Pedro Restrepo"))
                .andExpect(jsonPath("$.resultados[0].selloVerificacion").value("Verificado"));

        // 7. HU06: Perfil publico muestra sello verificado y NO expone documentoIdentidad
        MvcResult perfilRes = mockMvc.perform(get("/api/profesionales/" + idPedro)
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selloVerificacion").value("Verificado"))
                .andReturn();
        assertThat(perfilRes.getResponse().getContentAsString()).doesNotContain("documentoIdentidad");
    }

    @Test
    @DisplayName("HU03: Rechazo con motivo notifica al profesional y le permite volver a cargar documento")
    void rechazoYRecargaDocumento() throws Exception {
        String tokenAdmin = iniciarSesion("admin@alamano.co", "Admin1234");
        MockMultipartFile pdf = new MockMultipartFile(
                "documentoIdentidad", "doc.pdf", "application/pdf", "%PDF-1.4 test".getBytes()
        );

        MvcResult regRes = mockMvc.perform(multipart("/api/auth/registro/profesional")
                        .file(pdf)
                        .param("nombre", "Camila Ortiz")
                        .param("correo", "camila.pro@alamano.co")
                        .param("celular", "3182223344")
                        .param("contrasena", "ClavePro1")
                        .param("ubicacion", "Bello")
                        .param("latitud", "6.3373")
                        .param("longitud", "-75.5579")
                        .param("categoriasIds", "1")
                        .param("aceptaTerminos", "true"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode regJson = objectMapper.readTree(regRes.getResponse().getContentAsString());
        String tokenCamila = regJson.get("token").asText();
        Long idCamila = regJson.get("usuario").get("idUsuario").asLong();

        mockMvc.perform(post("/api/admin/profesionales/" + idCamila + "/rechazar")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"La foto de la cédula está borrosa.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVerificacion").value("RECHAZADO"))
                .andExpect(jsonPath("$.motivoRechazo").value("La foto de la cédula está borrosa."));

        mockMvc.perform(get("/api/notificaciones")
                        .header("Authorization", "Bearer " + tokenCamila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("VERIFICACION_RECHAZADA"));

        MockMultipartFile nuevoDoc = new MockMultipartFile(
                "documentoIdentidad", "cedula_clara.png", "image/png", new byte[]{1, 2, 3, 4}
        );
        mockMvc.perform(multipart("/api/profesionales/me/documentos")
                        .file(nuevoDoc)
                        .header("Authorization", "Bearer " + tokenCamila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVerificacion").value("PENDIENTE_VERIFICACION"));
    }

    private String iniciarSesion(String correo, String contrasena) throws Exception {
        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("correo", correo, "contrasena", contrasena)
        );
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
        MvcResult res = mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = objectMapper.readTree(res.getResponse().getContentAsString());
        for (JsonNode nodo : arr) {
            if (nombre.equals(nodo.get("nombre").asText())) {
                return nodo.get("idCategoria").asLong();
            }
        }
        throw new IllegalStateException("Categoría no encontrada: " + nombre);
    }
}
