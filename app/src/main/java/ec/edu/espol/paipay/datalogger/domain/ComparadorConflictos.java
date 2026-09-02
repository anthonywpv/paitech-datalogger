package ec.edu.espol.paipay.datalogger.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ec.edu.espol.paipay.datalogger.data.local.entity.JornadaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.MovimientoLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.ObservacionPezLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.CicloLombriculturaLocal;
import ec.edu.espol.paipay.datalogger.data.local.entity.RegistroLombriculturaLocal;
import ec.edu.espol.paipay.datalogger.data.local.model.JornadaConPeces;
import ec.edu.espol.paipay.datalogger.data.remote.dto.JornadaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CicloApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.MovimientoApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.CicloLombriculturaApiDto;
import ec.edu.espol.paipay.datalogger.data.remote.dto.RegistroLombriculturaApiDto;

/** Construye una comparación legible sin decidir por el usuario. */
public final class ComparadorConflictos {
    private ComparadorConflictos() { }

    public static String jornada(JornadaConPeces local, JornadaApiDto remota) {
        JornadaLocal j = local.jornada;
        String pecesLocales = resumenPecesLocales(local.peces);
        String pecesRemotos = resumenPecesRemotos(remota.peces);
        List<String> diferencias = new ArrayList<>();
        agregar(diferencias, "Piscina", j.piscinaCodigo, remota.piscinaCodigo);
        agregar(diferencias, "Fecha y hora", j.capturadaEn, remota.capturadaEn);
        agregar(diferencias, "Población estimada", texto(j.poblacionEstimada),
                texto(remota.poblacionEstimada));
        agregar(diferencias, "Observaciones", vacio(j.observaciones),
                vacio(remota.observaciones));
        agregar(diferencias, "pH", decimal(j.ph),
                remota.agua == null ? "Sin medición" : decimal(remota.agua.ph));
        agregar(diferencias, "Nitrato", decimal(j.nitrato),
                remota.agua == null ? "Sin medición" : decimal(remota.agua.nitrato));
        agregar(diferencias, "Nitrito", decimal(j.nitrito),
                remota.agua == null ? "Sin medición" : decimal(remota.agua.nitrito));
        agregar(diferencias, "Amoníaco total", decimal(j.amoniacoTotal),
                remota.agua == null ? "Sin medición" : decimal(remota.agua.amoniacoTotal));
        if (!pecesIguales(local.peces, remota.peces)) {
            diferencias.add("Biometría: teléfono «" + pecesLocales
                    + "» / servidor «" + pecesRemotos + "»");
        }

        return "TU CAMBIO EN EL TELÉFONO (basado en v" + j.versionServidor + ")\n"
                + resumenJornada(j.piscinaCodigo, j.capturadaEn, j.poblacionEstimada,
                j.observaciones, j.incluyeAgua, decimal(j.ph), decimal(j.nitrato),
                decimal(j.nitrito), decimal(j.amoniacoTotal), pecesLocales)
                + "\n\nVERSIÓN ACTUAL DEL SERVIDOR (v" + remota.version + ")\n"
                + resumenJornada(remota.piscinaCodigo, remota.capturadaEn,
                remota.poblacionEstimada, remota.observaciones, remota.agua != null,
                remota.agua == null ? null : decimal(remota.agua.ph),
                remota.agua == null ? null : decimal(remota.agua.nitrato),
                remota.agua == null ? null : decimal(remota.agua.nitrito),
                remota.agua == null ? null : decimal(remota.agua.amoniacoTotal), pecesRemotos)
                + "\n\nDIFERENCIAS DETECTADAS\n" + listaDiferencias(diferencias);
    }

    public static String movimiento(MovimientoLocal local, MovimientoApiDto remoto) {
        List<String> diferencias = new ArrayList<>();
        agregar(diferencias, "Tipo", local.tipo, remoto.tipo);
        agregar(diferencias, "Cantidad", texto(local.cantidad), texto(remoto.cantidad));
        agregar(diferencias, "Piscina de origen", vacio(local.piscinaOrigenUuid),
                vacio(remoto.piscinaOrigen));
        agregar(diferencias, "Piscina de destino", vacio(local.piscinaDestinoUuid),
                vacio(remoto.piscinaDestino));
        agregar(diferencias, "Fecha y hora", local.ocurridoEn, remoto.ocurridoEn);
        agregar(diferencias, "Observaciones", vacio(local.observaciones),
                vacio(remoto.observaciones));

        return "TU CAMBIO EN EL TELÉFONO (basado en v" + local.versionServidor + ")\n"
                + resumenMovimiento(local.tipo, local.cantidad, local.piscinaOrigenUuid,
                local.piscinaDestinoUuid, local.ocurridoEn, local.observaciones)
                + "\n\nVERSIÓN ACTUAL DEL SERVIDOR (v" + remoto.version + ")\n"
                + resumenMovimiento(remoto.tipo, remoto.cantidad, remoto.piscinaOrigen,
                remoto.piscinaDestino, remoto.ocurridoEn, remoto.observaciones)
                + "\n\nDIFERENCIAS DETECTADAS\n" + listaDiferencias(diferencias);
    }

    public static String ciclo(CicloLocal local, CicloApiDto remoto) {
        List<String> diferencias = new ArrayList<>();
        agregar(diferencias, "Identificador", local.uuid, remoto.id);
        agregar(diferencias, "Piscina", local.piscinaCodigo, remoto.piscinaCodigo);
        agregar(diferencias, "Estado", local.estado, remoto.estado);
        agregar(diferencias, "Fecha de inicio", local.iniciadoEn, remoto.iniciadoEn);
        agregar(diferencias, "Población inicial", texto(local.poblacionInicial),
                texto(remoto.poblacionInicial));
        agregar(diferencias, "Fecha de cierre", vacio(local.cerradoEn),
                vacio(remoto.cerradoEn));
        agregar(diferencias, "Destino de cierre", vacio(local.destinoCierre),
                vacio(remoto.destinoCierre));
        agregar(diferencias, "Población final", texto(local.poblacionFinal),
                texto(remoto.poblacionFinal));

        return "TU CICLO EN EL TELÉFONO (basado en v" + local.versionServidor + ")\n"
                + resumenCiclo(local.uuid, local.piscinaCodigo, local.estado,
                local.iniciadoEn, local.poblacionInicial, local.cerradoEn,
                local.destinoCierre, local.poblacionFinal)
                + "\n\nCICLO ACTUAL DEL SERVIDOR (v" + remoto.version + ")\n"
                + resumenCiclo(remoto.id, remoto.piscinaCodigo, remoto.estado,
                remoto.iniciadoEn, remoto.poblacionInicial, remoto.cerradoEn,
                remoto.destinoCierre, remoto.poblacionFinal)
                + "\n\nDIFERENCIAS DETECTADAS\n" + listaDiferencias(diferencias);
    }

    public static String registroLombricultura(RegistroLombriculturaLocal local,
                                                RegistroLombriculturaApiDto remoto) {
        List<String> diferencias = new ArrayList<>();
        agregar(diferencias, "Cama", local.camaCodigo, remoto.camaCodigo);
        agregar(diferencias, "Fecha y hora", local.capturadaEn, remoto.capturadaEn);
        agregar(diferencias, "pH del suelo", decimal(local.phSuelo),
                decimal(remoto.phSuelo));
        agregar(diferencias, "Conteo real", texto(local.conteoLombrices),
                texto(remoto.conteoLombrices));
        agregar(diferencias, "Observaciones", vacio(local.observaciones),
                vacio(remoto.observaciones));
        return "TU REGISTRO EN EL TELÉFONO (basado en v" + local.versionServidor + ")\n"
                + resumenRegistroLombricultura(local.camaCodigo, local.capturadaEn,
                decimal(local.phSuelo), local.conteoLombrices, local.observaciones)
                + "\n\nVERSIÓN ACTUAL DEL SERVIDOR (v" + remoto.version + ")\n"
                + resumenRegistroLombricultura(remoto.camaCodigo, remoto.capturadaEn,
                decimal(remoto.phSuelo), remoto.conteoLombrices, remoto.observaciones)
                + "\n\nDIFERENCIAS DETECTADAS\n" + listaDiferencias(diferencias);
    }

    public static String cicloLombricultura(CicloLombriculturaLocal local,
                                             CicloLombriculturaApiDto remoto) {
        List<String> diferencias = new ArrayList<>();
        agregar(diferencias, "Identificador", local.uuid, remoto.id);
        agregar(diferencias, "Cama", local.camaCodigo, remoto.camaCodigo);
        agregar(diferencias, "Estado", local.estado, remoto.estado);
        agregar(diferencias, "Fecha de inicio", local.iniciadoEn, remoto.iniciadoEn);
        agregar(diferencias, "Conteo inicial", texto(local.conteoInicial),
                texto(remoto.conteoInicial));
        agregar(diferencias, "Fecha de cierre", vacio(local.cerradoEn),
                vacio(remoto.cerradoEn));
        agregar(diferencias, "Conteo final", texto(local.conteoFinal),
                texto(remoto.conteoFinal));
        return "TU CICLO EN EL TELÉFONO (basado en v" + local.versionServidor + ")\n"
                + resumenCicloLombricultura(local.uuid, local.camaCodigo, local.estado,
                local.iniciadoEn, local.conteoInicial, local.cerradoEn, local.conteoFinal)
                + "\n\nCICLO ACTUAL DEL SERVIDOR (v" + remoto.version + ")\n"
                + resumenCicloLombricultura(remoto.id, remoto.camaCodigo, remoto.estado,
                remoto.iniciadoEn, remoto.conteoInicial, remoto.cerradoEn, remoto.conteoFinal)
                + "\n\nDIFERENCIAS DETECTADAS\n" + listaDiferencias(diferencias);
    }

    private static String resumenJornada(String piscina, String fecha, Integer poblacion,
                                          String observaciones, boolean incluyeAgua,
                                          String ph, String nitrato, String nitrito,
                                          String amoniacoTotal, String peces) {
        String agua = incluyeAgua
                ? "pH " + ph + ", nitrato " + nitrato + " ppm, nitrito " + nitrito + " ppm"
                + ", amoníaco total " + amoniacoTotal + " ppm"
                : "Sin bloque de agua";
        return "Piscina: " + vacio(piscina)
                + "\nFecha y hora: " + vacio(fecha)
                + "\nPoblación estimada: " + texto(poblacion)
                + "\nObservaciones: " + vacio(observaciones)
                + "\nAgua: " + agua
                + "\nBiometría: " + peces;
    }

    private static String resumenMovimiento(String tipo, int cantidad, String origen,
                                             String destino, String fecha,
                                             String observaciones) {
        return "Tipo: " + vacio(tipo)
                + "\nCantidad: " + cantidad
                + "\nPiscina de origen: " + vacio(origen)
                + "\nPiscina de destino: " + vacio(destino)
                + "\nFecha y hora: " + vacio(fecha)
                + "\nObservaciones: " + vacio(observaciones);
    }

    private static String resumenCiclo(String uuid, String piscina, String estado,
                                       String inicio, int poblacionInicial,
                                       String cierre, String destino,
                                       Integer poblacionFinal) {
        return "UUID: " + vacio(uuid)
                + "\nPiscina: " + vacio(piscina)
                + "\nEstado: " + vacio(estado)
                + "\nInicio: " + vacio(inicio)
                + "\nPoblación inicial: " + poblacionInicial
                + "\nCierre: " + vacio(cierre)
                + "\nDestino: " + vacio(destino)
                + "\nPoblación final: " + texto(poblacionFinal);
    }

    private static String resumenRegistroLombricultura(String cama, String fecha,
                                                        String ph, int conteo,
                                                        String observaciones) {
        return "Cama: " + vacio(cama)
                + "\nFecha y hora: " + vacio(fecha)
                + "\npH del suelo: " + ph
                + "\nConteo real: " + conteo
                + "\nObservaciones: " + vacio(observaciones);
    }

    private static String resumenCicloLombricultura(String uuid, String cama,
                                                     String estado, String inicio,
                                                     int conteoInicial, String cierre,
                                                     Integer conteoFinal) {
        return "UUID: " + vacio(uuid)
                + "\nCama: " + vacio(cama)
                + "\nEstado: " + vacio(estado)
                + "\nInicio: " + vacio(inicio)
                + "\nConteo inicial: " + conteoInicial
                + "\nCierre: " + vacio(cierre)
                + "\nConteo final: " + texto(conteoFinal);
    }

    private static String resumenPecesLocales(List<ObservacionPezLocal> peces) {
        if (peces == null || peces.isEmpty()) return "Sin peces medidos";
        double pesos = 0d;
        double tallas = 0d;
        for (ObservacionPezLocal pez : peces) {
            pesos += pez.pesoGramos;
            tallas += pez.tallaCentimetros;
        }
        return peces.size() + " pez/peces; promedio "
                + promedio(pesos, peces.size()) + " g y "
                + promedio(tallas, peces.size()) + " cm";
    }

    private static String resumenPecesRemotos(List<JornadaApiDto.PezDto> peces) {
        if (peces == null || peces.isEmpty()) return "Sin peces medidos";
        double pesos = 0d;
        double tallas = 0d;
        for (JornadaApiDto.PezDto pez : peces) {
            pesos += numero(pez.pesoGramos);
            tallas += numero(pez.tallaCentimetros);
        }
        return peces.size() + " pez/peces; promedio "
                + promedio(pesos, peces.size()) + " g y "
                + promedio(tallas, peces.size()) + " cm";
    }

    private static boolean pecesIguales(List<ObservacionPezLocal> locales,
                                         List<JornadaApiDto.PezDto> remotos) {
        int cantidadLocal = locales == null ? 0 : locales.size();
        int cantidadRemota = remotos == null ? 0 : remotos.size();
        if (cantidadLocal != cantidadRemota) return false;
        for (int i = 0; i < cantidadLocal; i++) {
            ObservacionPezLocal local = locales.get(i);
            JornadaApiDto.PezDto remoto = remotos.get(i);
            if (!Objects.equals(decimal(local.pesoGramos), decimal(remoto.pesoGramos))
                    || !Objects.equals(decimal(local.tallaCentimetros),
                    decimal(remoto.tallaCentimetros))) return false;
        }
        return true;
    }

    private static void agregar(List<String> diferencias, String campo,
                                String local, String remoto) {
        String izquierda = vacio(local);
        String derecha = vacio(remoto);
        if (!Objects.equals(izquierda, derecha)) {
            diferencias.add(campo + ": teléfono «" + izquierda
                    + "» / servidor «" + derecha + "»");
        }
    }

    private static String listaDiferencias(List<String> diferencias) {
        if (diferencias.isEmpty()) {
            return "No hay diferencias visibles. La versión cambió en el servidor y "
                    + "se mantiene la protección para evitar una sobrescritura automática.";
        }
        StringBuilder salida = new StringBuilder();
        for (String diferencia : diferencias) salida.append("• ").append(diferencia).append('\n');
        return salida.toString().trim();
    }

    private static String promedio(double total, int cantidad) {
        return BigDecimal.valueOf(total / cantidad).setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    private static String decimal(Double valor) {
        return valor == null ? "Sin medición" : decimal(String.valueOf(valor));
    }

    private static String decimal(double valor) { return decimal(String.valueOf(valor)); }

    private static String decimal(String valor) {
        if (valor == null || valor.trim().isEmpty()) return "Sin medición";
        try { return new BigDecimal(valor).stripTrailingZeros().toPlainString(); }
        catch (NumberFormatException error) { return valor; }
    }

    private static double numero(String valor) {
        if (valor == null || valor.trim().isEmpty()) return 0d;
        return Double.parseDouble(valor);
    }

    private static String texto(Object valor) {
        return valor == null ? "Sin dato" : String.valueOf(valor);
    }

    private static String vacio(String valor) {
        return valor == null || valor.trim().isEmpty() ? "Sin dato" : valor;
    }
}
