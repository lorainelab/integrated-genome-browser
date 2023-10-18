/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.affymetrix.fx;

import javafx.embed.swing.JFXPanel;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
/**
 *
 * @author dcnorris
 */

@Component(immediate = true)
public class OpenJfxBootstrap {

    @Activate
    public void activate() {
        new JFXPanel();  // This will initialize the JavaFX runtime
    }
}
