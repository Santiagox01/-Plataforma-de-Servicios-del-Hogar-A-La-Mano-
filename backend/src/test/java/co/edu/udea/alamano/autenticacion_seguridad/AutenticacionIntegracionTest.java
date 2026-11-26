package co.edu.udea.alamano.autenticacion_seguridad;

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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacionIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("HU01: Registro exitoso de cliente, rechazo por correo repetido y campos inválidos")
    void flujoRegistroCliente() throws Exception {
        String cuerpoValido = """
                {
                  "nombre": "Sofía Montoya",
                  "correo": "sofia.nueva@alamano.co",
                  "celular": "3019998877",
                  "contrasena": "Password1",
                  "ubicacion": "Envigado, Antioquia",
                  "latitud": 6.1719,
                  "longitud": -75.5863,
                  "aceptaTerminos": true
                }
                """;

        mockMvc.perform(post("/api/auth/registro/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoValido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.usuario.correo").value("sofia.nueva@alamano.co"));

        mockMvc.perform(post("/api/auth/registro/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoValido))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo electrónico ya está en uso."));

        String cuerpoInvalido = """
                {
                  "nombre": "",
                  "correo": "sofia2@alamano.co",
                  "celular": "3019998877",
                  "contrasena": "Password1",
                  "ubicacion": "Medellín",
                  "latitud": 6.2,
                  "longitud": -75.5,
                  "aceptaTerminos": false
                }
                """;

        mockMvc.perform(post("/api/auth/registro/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU02: Registro de profesional exige documento JPG/PNG/PDF válido y queda pendiente")
    void flujoRegistroProfesional() throws Exception {
        MockMultipartFile pdfValido = new MockMultipartFile(
                "documentoIdentidad", "cedula.pdf", "application/pdf", "%PDF-1.4 contenido".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/auth/registro/profesional")
                        .file(pdfValido)
                        .param("nombre", "Julián Cano")
                        .param("correo", "julian.pro@alamano.co")
                        .param("celular", "3157778899")
                        .param("contrasena", "ClavePro1")
                        .param("ubicacion", "Robledo, Medellín")
                        .param("latitud", "6.2760")
                        .param("longitud", "-75.5910")
                        .param("categoriasIds", "1")
                        .param("aceptaTerminos", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.rol").value("PROFESIONAL"))
                .andExpect(jsonPath("$.usuario.estadoVerificacion").value("PENDIENTE_VERIFICACION"))
                .andExpect(jsonPath("$.usuario.disponible").value(false))
                .andExpect(jsonPath("$.aviso").isNotEmpty());

        MockMultipartFile archivoInvalido = new MockMultipartFile(
                "documentoIdentidad", "script.txt", "text/plain", "hola".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/auth/registro/profesional")
                        .file(archivoInvalido)
                        .param("nombre", "Otro Pro")
                        .param("correo", "otro.pro@alamano.co")
                        .param("celular", "3157778800")
                        .param("contrasena", "ClavePro1")
                        .param("ubicacion", "Medellín")
                        .param("latitud", "6.27")
                        .param("longitud", "-75.59")
                        .param("categoriasIds", "1")
                        .param("aceptaTerminos", "true"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU04: Login de profesional pendiente muestra aviso, bloqueo tras 5 fallos y logout revoca JWT")
    void flujoLoginBloqueoYLogout() throws Exception {
        String loginPendiente = """
                { "correo": "pedro.cerrajero@alamano.co", "contrasena": "Profesional1" }
                """;
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPendiente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.estadoVerificacion").value("PENDIENTE_VERIFICACION"))
                .andExpect(jsonPath("$.aviso").value("Tu cuenta está en verificación. Aún no puedes recibir solicitudes hasta ser aprobada."));

        String registroBloqueo = """
                {
                  "nombre": "Usuario Prueba Bloqueo",
                  "correo": "bloqueo.test@alamano.co",
                  "celular": "3024445566",
                  "contrasena": "Password1",
                  "ubicacion": "Medellín",
                  "latitud": 6.2447,
                  "longitud": -75.5950,
                  "aceptaTerminos": true
                }
                """;
        mockMvc.perform(post("/api/auth/registro/cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroBloqueo))
                .andExpect(status().isCreated());

        String loginInvalido = """
                { "correo": "bloqueo.test@alamano.co", "contrasena": "ClaveIncorrecta9" }
                """;
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginInvalido))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginInvalido))
                .andExpect(status().isLocked());

        String loginValidoCarlos = """
                { "correo": "carlos.cliente@alamano.co", "contrasena": "Cliente123" }
                """;
        MvcResult loginOk = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginValidoCarlos))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginOk.getResponse().getContentAsString());
        String token = json.get("token").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("carlos.cliente@alamano.co"));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
