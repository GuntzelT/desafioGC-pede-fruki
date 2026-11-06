package br.com.desafiogc.pedi_fruki_api.service;

/** Categorias usadas pelas regras de clima, feriados e sazonalidade. */
public enum CategoriaBebidaService {
    ALCOOLICA("bebidas alcoólicas"),
    REFRIGERANTE("refrigerantes"),
    AGUA("água"),
    OUTRAS("outras bebidas");

    private final String rotulo;

    CategoriaBebidaService(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}