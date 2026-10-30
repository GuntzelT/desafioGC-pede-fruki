package br.valueobject;

import java.util.Objects;

//normalização e validação de formato do CNPJ. */
public final class Cnpj {
    private final String numero;

    public Cnpj(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Informe o CNPJ.");
        }
        String digitos = texto.replaceAll("[^0-9]", "");
        if (digitos.length() != 14) {
            throw new IllegalArgumentException("O CNPJ deve ter exatamente 14 dígitos.");
        }
        this.numero = digitos;
    }

    public String getNumero() {
        return numero;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof Cnpj)) {
            return false;
        }
        Cnpj outroCnpj = (Cnpj) outro;
        return Objects.equals(numero, outroCnpj.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return numero;
    }
}
