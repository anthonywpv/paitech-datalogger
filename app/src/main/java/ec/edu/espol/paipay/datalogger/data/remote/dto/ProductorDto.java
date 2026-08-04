package ec.edu.espol.paipay.datalogger.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Perfil del productor almacenado en public.perfil_productor.
 *
 * La autenticación (usuario, contraseña, sesiones) la maneja Neon Auth, no
 * esta tabla. Aquí solo vive información propia del proyecto: a qué comuna
 * pertenece el productor y qué rol cumple dentro del recinto.
 */
public class ProductorDto {

    @SerializedName("user_id") public String userId;
    @SerializedName("nombre")  public String nombre;
    @SerializedName("comuna")  public String comuna;
    @SerializedName("rol")     public String rol;
    @SerializedName("activo")  public Boolean activo;
}
