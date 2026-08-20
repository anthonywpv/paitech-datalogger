package ec.edu.espol.paipay.datalogger.data.repo;

import java.util.ArrayList;
import java.util.List;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.PiscinaLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.data.remote.dto.JornadaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CicloApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CierreCicloDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.MovimientoApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.PiscinaApiDto;
import ec.edu.espol.paipay.datalogger.util.SeguridadUtil;
import ec.edu.espol.paipay.datalogger.util.FechaUtil;

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
        local.cicloActivoUuid = dto.cicloActivo == null ? null : dto.cicloActivo.id;
        local.cicloActivoNumero = dto.cicloActivo == null ? null : dto.cicloActivo.numero;
        local.recordatorioAguaEstado = dto.recordatorios == null || dto.recordatorios.agua == null
                ? null : dto.recordatorios.agua.estado;
        local.recordatorioBiometriaEstado = dto.recordatorios == null
                || dto.recordatorios.biometria == null
                ? null : dto.recordatorios.biometria.estado;
        local.recordatoriosActualizadosEn = FechaUtil.isoUtc(System.currentTimeMillis());
        return local;
    }

    public static JornadaApiDto aDto(JornadaConPeces local, String dispositivoId) {
        JornadaApiDto dto = new JornadaApiDto();
        JornadaLocal j = local.jornada;
        dto.id = j.uuid;
        dto.piscina = j.piscinaUuid;
        dto.ciclo = j.cicloUuid;
        dto.capturadaEn = j.capturadaEn;
        dto.poblacionEstimada = j.poblacionEstimada;
        dto.observaciones = j.observaciones == null ? "" : j.observaciones;
        dto.dispositivoId = dispositivoId;
        dto.version = j.versionServidor > 0 ? j.versionServidor : null;
        dto.motivoCorreccion = j.motivoCambio == null ? "" : j.motivoCambio;
        if (j.incluyeAgua) {
            dto.agua = new JornadaApiDto.AguaDto();
            dto.agua.ph = numero(j.ph);
            dto.agua.nitrato = numero(j.nitrato);
            dto.agua.nitrito = numero(j.nitrito);
            dto.agua.amoniacoTotal = numero(j.amoniacoTotal);
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
        j.cicloUuid = dto.ciclo;
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
            j.amoniacoTotal = decimal(dto.agua.amoniacoTotal);
        }
        j.estadoLocal = "ANULADA".equals(dto.estado)
                ? JornadaLocal.ANULADO : JornadaLocal.SINCRONIZADO;
        j.versionServidor = dto.version == null ? 0 : dto.version;
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
        dto.cicloOrigen = local.cicloOrigenUuid;
        dto.cicloDestino = local.cicloDestinoUuid;
        dto.ocurridoEn = local.ocurridoEn;
        dto.observaciones = local.observaciones;
        dto.version = local.versionServidor > 0 ? local.versionServidor : null;
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
        local.cicloOrigenUuid = dto.cicloOrigen;
        local.cicloDestinoUuid = dto.cicloDestino;
        local.ocurridoEn = dto.ocurridoEn;
        local.observaciones = dto.observaciones;
        local.estadoLocal = "ANULADO".equals(dto.estado)
                ? JornadaLocal.ANULADO : JornadaLocal.SINCRONIZADO;
        local.versionServidor = dto.version == null ? 0 : dto.version;
        local.autorCorreo = dto.autor != null && dto.autor.correo != null
                ? dto.autor.correo : correoSesion;
        local.creadaEn = System.currentTimeMillis();
        local.modificadaEn = System.currentTimeMillis();
        return local;
    }

    public static CicloApiDto aDto(CicloLocal local, String dispositivoId) {
        CicloApiDto dto = new CicloApiDto();
        dto.id = local.uuid;
        dto.piscina = local.piscinaUuid;
        dto.iniciadoEn = local.iniciadoEn;
        dto.poblacionInicial = local.poblacionInicial;
        dto.duracionEstimadaMeses = local.duracionEstimadaMeses;
        dto.observacionesApertura = local.observacionesApertura;
        dto.dispositivoId = dispositivoId;
        if (local.prediccionCiclosUsados > 0 && local.prediccionPoblacionFinal != null) {
            CicloApiDto.PrediccionCacheDto p = new CicloApiDto.PrediccionCacheDto();
            p.poblacionFinal = local.prediccionPoblacionFinal;
            p.minimo = local.prediccionMin;
            p.maximo = local.prediccionMax;
            p.tasa = String.valueOf(local.prediccionTasa);
            p.ciclosUsados = local.prediccionCiclosUsados;
            p.confianza = local.prediccionConfianza;
            p.metodoVersion = local.prediccionMetodoVersion;
            p.calculadaEn = local.prediccionCalculadaEn;
            p.datosHasta = local.prediccionDatosHasta;
            dto.prediccionCache = p;
        }
        return dto;
    }

    public static CierreCicloDto cierreDto(CicloLocal local) {
        CierreCicloDto dto = new CierreCicloDto();
        dto.version = local.versionServidor;
        dto.cerradoEn = local.cerradoEn;
        dto.destinoCierre = local.destinoCierre;
        dto.poblacionFinal = local.poblacionFinal == null ? 0 : local.poblacionFinal;
        dto.pesoTotalCosechadoKg = local.pesoTotalCosechadoKg == null
                ? null : String.valueOf(local.pesoTotalCosechadoKg);
        dto.observacionesCierre = local.observacionesCierre;
        dto.piscinaDestinoCierre = local.piscinaDestinoCierreUuid;
        return dto;
    }

    public static CicloLocal aLocal(CicloApiDto dto, String correoSesion) {
        CicloLocal local = new CicloLocal();
        local.uuid = dto.id;
        local.piscinaUuid = dto.piscina;
        local.piscinaCodigo = dto.piscinaCodigo == null ? "" : dto.piscinaCodigo;
        local.especieNombre = dto.especie == null ? null : dto.especie.nombreComun;
        local.numero = dto.numero;
        local.estado = dto.estado;
        local.iniciadoEn = dto.iniciadoEn;
        local.poblacionInicial = dto.poblacionInicial;
        local.duracionEstimadaMeses = dto.duracionEstimadaMeses;
        local.observacionesApertura = dto.observacionesApertura;
        local.autorCorreo = dto.autorApertura != null && dto.autorApertura.correo != null
                ? dto.autorApertura.correo : correoSesion;
        local.cerradoEn = dto.cerradoEn;
        local.destinoCierre = dto.destinoCierre;
        local.poblacionFinal = dto.poblacionFinal;
        local.pesoTotalCosechadoKg = nullableDecimal(dto.pesoTotalCosechadoKg);
        local.observacionesCierre = dto.observacionesCierre;
        local.piscinaDestinoCierreUuid = dto.piscinaDestinoCierre;
        if (dto.prediccion != null) {
            local.prediccionPoblacionFinal = dto.prediccion.poblacionFinal;
            local.prediccionMin = dto.prediccion.minimo;
            local.prediccionMax = dto.prediccion.maximo;
            local.prediccionTasa = nullableDecimal(dto.prediccion.tasa);
            local.prediccionCiclosUsados = dto.prediccion.ciclosUsados;
            local.prediccionConfianza = dto.prediccion.confianza;
            local.prediccionMetodoVersion = dto.prediccion.metodoVersion;
            local.prediccionCalculadaEn = dto.prediccion.calculadaEn;
            local.prediccionDatosHasta = dto.prediccion.datosHasta;
            local.prediccionOrigen = dto.prediccion.origen;
        }
        local.estadoLocal = CicloLocal.SINCRONIZADO;
        local.versionServidor = dto.version == null ? 0 : dto.version;
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
    private static Double nullableDecimal(String valor) {
        return valor == null || valor.isEmpty() ? null : Double.valueOf(valor);
    }
}
