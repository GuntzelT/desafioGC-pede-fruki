package br.valueobject;

//Value Object que entende a identificação digitada no WhatsApp simulado: 14 dígitos representam CNPJ; os demais, um ID numérico de cliente

public final class IdentificadorCliente {
    private final Cnpj cnpj;
    private final Integer idCliente;

    public IdentificadorCliente(String entrada) {
        String somenteDigitos = "";
        if (entrada != null) {
            somenteDigitos = entrada.replaceAll("[^0-9]", "");
        }
        if (somenteDigitos.isBlank()) {
            throw new IllegalArgumentException("Informe um CNPJ com 14 dígitos ou o ID do cliente.");
        }

        if (somenteDigitos.length() == 14) {
            this.cnpj = new Cnpj(somenteDigitos);
            this.idCliente = null;
        } else {
            int valor;
            try {
                valor = Integer.parseInt(somenteDigitos);
            } catch (NumberFormatException erro) {
                throw new IllegalArgumentException("O código do cliente não é um inteiro válido.", erro);
            }
            if (valor <= 0) {
                throw new IllegalArgumentException("O ID do cliente deve ser positivo.");
            }
            this.cnpj = null;
            this.idCliente = valor;
        }
    }

    public boolean ehCnpj() {
        return cnpj != null;
    }

    public Cnpj getCnpj() {
        return cnpj;
    }

    public Integer getIdCliente() {
        return idCliente;
    }
}
