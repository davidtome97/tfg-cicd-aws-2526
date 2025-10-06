package com.sistemagestionapp.controller;

import com.sistemagestionapp.model.Bloque;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la funcionalidad Blockchain de la aplicación.
 * Aquí gestionamos la creación y visualización de bloques que simulan una cadena de bloques simple.
 */
@Controller
@RequestMapping("/blockchain")
public class BlockchainController {

    /**
     * Lista que representa nuestra cadena de bloques.
     */
    private final List<Bloque> blockchain = new ArrayList<>();

    /**
     * Constructor que inicializa el primer bloque (bloque génesis) al arrancar la aplicación.
     */
    public BlockchainController() {
        blockchain.add(new Bloque(0,"Bloque principal", "0"));
    }

    /**
     * Muestra la página de la cadena de bloques con los bloques actuales y una indicación de validez.
     * @param model Objeto para pasar atributos a la vista
     * @return nombre de la plantilla HTML "blockchain"
     */
    @GetMapping
    public String verBlockchain(Model model) {
        model.addAttribute("blockchain", blockchain);
        model.addAttribute("valida", esCadenaValida());
        return "blockchain";
    }

    /**
     * Agrega un nuevo bloque con los datos proporcionados por el usuario.
     * @param datos Contenido del nuevo bloque
     * @return redirección a la vista actualizada de la cadena
     */
    @PostMapping("/agregar")
    public String agregarBloque(@RequestParam String datos) {
        Bloque ultimo = blockchain.get(blockchain.size() - 1);
        int nuevoIndice = blockchain.size();
        Bloque nuevo = new Bloque(nuevoIndice, datos, ultimo.getHash());
        blockchain.add(nuevo);
        return "redirect:/blockchain";
    }

    /**
     * Verifica si la cadena de bloques es válida comprobando los hashes.
     * @return true si la cadena es válida, false en caso contrario
     */
    public boolean esCadenaValida() {
        for (int i = 1; i < blockchain.size(); i++) {
            Bloque actual = blockchain.get(i);
            Bloque anterior = blockchain.get(i - 1);

            if (!actual.getHash().equals(actual.calcularHash())) {
                return false;
            }

            if (!actual.getHashAnterior().equals(anterior.getHash())) {
                return false;
            }
        }
        return true;
    }
}