package ec.edu.espol.paipay.datalogger.data.remote.dto;

public class LoginRespuestaDto {
    public String token;
    public UsuarioDto usuario;

    public static class UsuarioDto {
        public long id;
        public String correo;
        public String nombre;
        public String rol;
        public ComunidadDto comunidad;
    }

    public static class ComunidadDto {
        public String codigo;
        public String nombre;
    }
}
