package ec.edu.espol.paipay.datalogger.data.repo;

import java.util.ArrayList;
import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.data.remote.dto.JornadaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.MovimientoApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.PiscinaApiDto;
import ec.edu.espol.paipay.datalogger.util.SeguridadUtil;

public final class MapeadorApi {
    private MapeadorApi() { }

    public static PiscinaLocal piscina(PiscinaApiDto dto) {
        PiscinaLocal local = new PiscinaLocal();
        local.uuid = dto.id;
        local.codigo = dto.codigo;
        local.nombre = dto.nombre;
        local.tipo = dto.tipo;
        local.descripcion = dto.descripcion;
        local.especieNombre = dto.especie == null ? null : dto.especie.nombreComun;
        local.activa = true;
        return local;
    }

    public static JornadaApiDto aDto(JornadaConPeces local, String dispositivoId) {
        JornadaApiDto dto = new JornadaApiDto();
        JornadaLocal j = local.jornada;
        dto.id = j.uuid;
        dto.piscina = j.piscinaUuid;
        dto.capturadaEn = j.capturadaEn;
        dto.poblacionEstimada = j.poblacionEstimada;
        dto.observaciones = j.observaciones == null ? "" : j.observaciones;
        dto.dispositivoId = dispositivoId;
        dto.version = j.versionServidor;
        dto.motivoCorreccion = j.motivoCambio == null ? "" : j.motivoCambio;
        if (j.incluyeAgua) {
            dto.agua = new JornadaApiDto.AguaDto();
            dto.agua.ph = numero(j.ph);
            dto.agua.nitrato = numero(j.nitrato);
            dto.agua.nitrito = numero(j.nitrito);
            dto.agua.amonio = numero(j.amonio);
        }
        dto.peces = new ArrayList<>();
        if (local.peces != null) {
            for (ObservacionPezLocal pez : local.peces) {
                JornadaApiDto.PezDto p = new JornadaApiDto.PezDto();
                p.pesoGramos = numero(pez.pesoGramos);
                p.tallaCentimetros = numero(pez.tallaCentimetros);
                dto.peces.add(p);
            }
        }
        return dto;
    }

    public static JornadaConPeces aLocal(JornadaApiDto dto, PiscinaLocal piscina,
                                          String correoSesion) {
        JornadaConPeces resultado = new JornadaConPeces();
        JornadaLocal j = new JornadaLocal();
        j.uuid = dto.id;
        j.piscinaUuid = dto.piscina;
        j.piscinaCodigo = dto.piscinaCodigo != null ? dto.piscinaCodigo
                : piscina == null ? "" : piscina.codigo;
        j.especieNombre = dto.especie != null ? dto.especie
                : piscina == null ? null : piscina.especieNombre;
        j.autorCorreo = dto.autor != null && dto.autor.correo != null
                ? dto.autor.correo : correoSesion;
        j.capturadaEn = dto.capturadaEn;
        j.poblacionEstimada = dto.poblacionEstimada;
        j.observaciones = dto.observaciones;
        j.incluyeAgua = dto.agua != null;
        if (dto.agua != null) {
            j.ph = decimal(dto.agua.ph);
            j.nitrato = decimal(dto.agua.nitrato);
            j.nitrito = decimal(dto.agua.nitrito);
            j.amonio = decimal(dto.agua.amonio);
        }
        j.estadoLocal = "ANULADA".equals(dto.estado)
                ? JornadaLocal.ANULADO : JornadaLocal.SINCRONIZADO;
        j.versionServidor = dto.version;
        j.creadaEn = System.currentTimeMillis();
        j.modificadaEn = System.currentTimeMillis();
        resultado.jornada = j;
        resultado.peces = new ArrayList<>();
        if (dto.peces != null) {
            int orden = 1;
            for (JornadaApiDto.PezDto remoto : dto.peces) {
                ObservacionPezLocal pez = new ObservacionPezLocal();
                pez.uuid = remoto.id == null ? SeguridadUtil.nuevoUuid() : remoto.id;
                pez.jornadaUuid = j.uuid;
                pez.orden = remoto.orden > 0 ? remoto.orden : orden;
                pez.pesoGramos = decimal(remoto.pesoGramos);
                pez.tallaCentimetros = decimal(remoto.tallaCentimetros);
                resultado.peces.add(pez);
                orden++;
            }
        }
        return resultado;
    }

    public static MovimientoApiDto aDto(MovimientoLocal local) {
        MovimientoApiDto dto = new MovimientoApiDto();
        dto.id = local.uuid;
        dto.tipo = local.tipo;
        dto.cantidad = local.cantidad;
        dto.piscinaOrigen = local.piscinaOrigenUuid;
        dto.piscinaDestino = local.piscinaDestinoUuid;
        dto.ocurridoEn = local.ocurridoEn;
        dto.observaciones = local.observaciones;
        dto.version = local.versionServidor;
        dto.motivoCorreccion = local.motivoCambio;
        return dto;
    }

    public static MovimientoLocal aLocal(MovimientoApiDto dto, String correoSesion) {
        MovimientoLocal local = new MovimientoLocal();
        local.uuid = dto.id;
        local.tipo = dto.tipo;
        local.cantidad = dto.cantidad;
        local.piscinaOrigenUuid = dto.piscinaOrigen;
        local.piscinaDestinoUuid = dto.piscinaDestino;
        local.ocurridoEn = dto.ocurridoEn;
        local.observaciones = dto.observaciones;
        local.estadoLocal = "ANULADO".equals(dto.estado)
                ? JornadaLocal.ANULADO : JornadaLocal.SINCRONIZADO;
        local.versionServidor = dto.version;
        local.autorCorreo = correoSesion;
        local.creadaEn = System.currentTimeMillis();
        local.modificadaEn = System.currentTimeMillis();
        return local;
    }

    private static String numero(Double valor) {
        return valor == null ? null : String.valueOf(valor);
    }
    private static String numero(double valor) { return String.valueOf(valor); }
    private static double decimal(String valor) {
        if (valor == null || valor.isEmpty()) return 0d;
        return Double.parseDouble(valor);
    }
}
