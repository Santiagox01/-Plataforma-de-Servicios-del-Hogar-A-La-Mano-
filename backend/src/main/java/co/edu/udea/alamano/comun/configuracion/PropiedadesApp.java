package co.edu.udea.alamano.comun.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class PropiedadesApp {

    private String publicBaseUrl = "http://localhost:8080";
    private Jwt jwt = new Jwt();
    private Autenticacion autenticacion = new Autenticacion();
    private Storage storage = new Storage();
    private Cors cors = new Cors();
    private Pagos pagos = new Pagos();
    private Moderacion moderacion = new Moderacion();

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Autenticacion getAutenticacion() {
        return autenticacion;
    }

    public void setAutenticacion(Autenticacion autenticacion) {
        this.autenticacion = autenticacion;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Pagos getPagos() {
        return pagos;
    }

    public void setPagos(Pagos pagos) {
        this.pagos = pagos;
    }

    public Moderacion getModeracion() {
        return moderacion;
    }

    public void setModeracion(Moderacion moderacion) {
        this.moderacion = moderacion;
    }

    public static class Jwt {
        private String secret;
        private long expiracionMs = 86400000L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpiracionMs() {
            return expiracionMs;
        }

        public void setExpiracionMs(long expiracionMs) {
            this.expiracionMs = expiracionMs;
        }
    }

    public static class Autenticacion {
        private int maxIntentosFallidos = 5;
        private int minutosBloqueo = 15;

        public int getMaxIntentosFallidos() {
            return maxIntentosFallidos;
        }

        public void setMaxIntentosFallidos(int maxIntentosFallidos) {
            this.maxIntentosFallidos = maxIntentosFallidos;
        }

        public int getMinutosBloqueo() {
            return minutosBloqueo;
        }

        public void setMinutosBloqueo(int minutosBloqueo) {
            this.minutosBloqueo = minutosBloqueo;
        }
    }

    public static class Storage {
        private String tipo = "local";
        private String localDir = "./uploads";
        private String firmaSecret;
        private long expiracionUrlSegundos = 300L;
        private String supabaseUrl = "";
        private String supabaseServiceRoleKey = "";
        private String supabaseBucket = "documentos-privados";

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public String getLocalDir() {
            return localDir;
        }

        public void setLocalDir(String localDir) {
            this.localDir = localDir;
        }

        public String getFirmaSecret() {
            return firmaSecret;
        }

        public void setFirmaSecret(String firmaSecret) {
            this.firmaSecret = firmaSecret;
        }

        public long getExpiracionUrlSegundos() {
            return expiracionUrlSegundos;
        }

        public void setExpiracionUrlSegundos(long expiracionUrlSegundos) {
            this.expiracionUrlSegundos = expiracionUrlSegundos;
        }

        public String getSupabaseUrl() {
            return supabaseUrl;
        }

        public void setSupabaseUrl(String supabaseUrl) {
            this.supabaseUrl = supabaseUrl;
        }

        public String getSupabaseServiceRoleKey() {
            return supabaseServiceRoleKey;
        }

        public void setSupabaseServiceRoleKey(String supabaseServiceRoleKey) {
            this.supabaseServiceRoleKey = supabaseServiceRoleKey;
        }

        public String getSupabaseBucket() {
            return supabaseBucket;
        }

        public void setSupabaseBucket(String supabaseBucket) {
            this.supabaseBucket = supabaseBucket;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:8081,http://localhost:19006";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Pagos {
        private String pasarela = "simulada";
        private Paypal paypal = new Paypal();

        public String getPasarela() {
            return pasarela;
        }

        public void setPasarela(String pasarela) {
            this.pasarela = pasarela;
        }

        public Paypal getPaypal() {
            return paypal;
        }

        public void setPaypal(Paypal paypal) {
            this.paypal = paypal;
        }
    }

    public static class Paypal {
        private String clientId = "";
        private String clientSecret = "";
        private String baseUrl = "https://api-m.sandbox.paypal.com";
        private String returnUrl = "http://localhost:8080/api/pagos/paypal/retorno";
        private String cancelUrl = "http://localhost:8080/api/pagos/paypal/cancelado";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getReturnUrl() {
            return returnUrl;
        }

        public void setReturnUrl(String returnUrl) {
            this.returnUrl = returnUrl;
        }

        public String getCancelUrl() {
            return cancelUrl;
        }

        public void setCancelUrl(String cancelUrl) {
            this.cancelUrl = cancelUrl;
        }

        @Override
        public String toString() {
            return "Paypal[baseUrl=" + baseUrl + ", returnUrl=" + returnUrl + ", cancelUrl=" + cancelUrl + "]";
        }
    }

    public static class Moderacion {
        private int diasSuspensionDefecto = 7;

        public int getDiasSuspensionDefecto() {
            return diasSuspensionDefecto;
        }

        public void setDiasSuspensionDefecto(int diasSuspensionDefecto) {
            this.diasSuspensionDefecto = diasSuspensionDefecto;
        }
    }
}
