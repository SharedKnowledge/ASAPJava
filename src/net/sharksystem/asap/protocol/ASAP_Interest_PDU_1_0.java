package net.sharksystem.asap.protocol;

public interface ASAP_Interest_PDU_1_0 extends ASAP_PDU_1_0 {
    boolean eraFromSet();
    boolean eraToSet();
    int getEraFrom();
    int getEraTo();
}
