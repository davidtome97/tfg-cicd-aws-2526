package com.sistemagestionapp.service;

import com.sistemagestionapp.model.Bloque;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Esta clase la utilizo para gestionar una cadena de bloques (blockchain) simple.
 * Me encargo de inicializar la blockchain con un bloque inicial y de añadir nuevos bloques
 * de forma secuencial, manteniendo la integridad de la cadena mediante el uso de hashes.
 *
 * Cada bloque se enlaza con el anterior utilizando el hash del bloque anterior.
 *
 * @author David Tomé Arnáiz
 */
@Service
public class BlockchainService {

    /**
     * Aquí guardo toda la cadena de bloques. Uso una lista para mantener el orden de los bloques.
     */
    private final List<Bloque> blockchain = new ArrayList<>();

    /**
     * Cuando se instancia este servicio, añado automáticamente el bloque génesis a la cadena.
     */
    public BlockchainService() {
        blockchain.add(new Bloque(0, "Bloque", "0"));
    }

    /**
     * Devuelvo la cadena completa de bloques.
     *
     * @return lista de objetos {@link Bloque} que forman la blockchain.
     */
    /*public List<Bloque> obtenerBlockchain() {
        return blockchain;
    }*/

    /**
     * Añado un nuevo bloque a la cadena utilizando los datos que me pasan como argumento.
     * El nuevo bloque se enlaza al último de la lista usando su hash.
     *
     * @param datos información que quiero almacenar en el nuevo bloque.
     */
    /*public void añadirBloque(String datos) {
        Bloque anterior = blockchain.get(blockchain.size() - 1);
        Bloque nuevo = new Bloque(blockchain.size(), datos, anterior.getHash());
        blockchain.add(nuevo);
    }*/
}