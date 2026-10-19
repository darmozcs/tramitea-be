package com.darmoz.tramitea.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/** En JSON y en la base viaja el codigo de dos letras (igual que el enum del front). */
public enum UruguayDepartment {
    ARTIGAS("AR"), CANELONES("CA"), CERRO_LARGO("CL"), COLONIA("CO"),
    DURAZNO("DU"), FLORES("FS"), FLORIDA("FD"), LAVALLEJA("LA"),
    MALDONADO("MA"), MONTEVIDEO("MO"), PAYSANDU("PA"), RIO_NEGRO("RN"),
    RIVERA("RV"), ROCHA("RO"), SALTO("SA"), SAN_JOSE("SJ"),
    SORIANO("SO"), TACUAREMBO("TA"), TREINTA_Y_TRES("TT");

    private final String code;

    UruguayDepartment(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static UruguayDepartment fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(department -> department.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Departamento desconocido: " + code));
    }
}
