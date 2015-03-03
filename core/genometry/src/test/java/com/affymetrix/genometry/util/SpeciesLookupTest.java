/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.util;

import java.io.IOException;
import java.io.InputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author sgblanch
 */
public class SpeciesLookupTest {

    private static final String c_brenneri = "Caenorhabditis brenneri";
    private static final String m_musculus = "Mus musculus";
    private static final String p_pygmaeus_abelii = "Pongo pygmaeus abelii";

    /**
     * Test of getSpeciesName method, of class SpeciesLookup.
     *
     * @throws IOException
     */
    @Test
    public void testGetSpeciesName() throws IOException {
        String filename = "data/speciesLookup/species.txt";

        InputStream istr = SpeciesLookupTest.class.getClassLoader().getResourceAsStream(filename);
        assertNotNull(istr);

        SpeciesLookup.load(istr);

        String version = "C_brenneri_Aug_2009";
        String result = SpeciesLookup.getSpeciesName(version, true);
        assertEquals(c_brenneri, result);

        version = "caePb9";
        result = SpeciesLookup.getSpeciesName(version, true);
        assertEquals(c_brenneri, result);

        version = "c_Brenneri_Aug_2009";
        result = SpeciesLookup.getSpeciesName(version, true);
        assertEquals("C_brenneri", result);
        result = SpeciesLookup.getSpeciesName(version, false);
        assertEquals(c_brenneri, result);

        version = "caepb9";
        result = SpeciesLookup.getSpeciesName(version, true);
        assertEquals("caepb", result);
        result = SpeciesLookup.getSpeciesName(version, false);
        assertEquals(c_brenneri, result);

        version = "mm9";
        result = SpeciesLookup.getSpeciesName(version, true);
        assertEquals(m_musculus, result);

        version = "MM9";
        result = SpeciesLookup.getSpeciesName(version, true);
        assertEquals("MM", result);
        result = SpeciesLookup.getSpeciesName(version, false);
        assertEquals(m_musculus, result);

        version = "ponAbe9";
        result = SpeciesLookup.getSpeciesName(version, true);
        assertEquals(p_pygmaeus_abelii, result);

        version = "ponabe9";
        result = SpeciesLookup.getSpeciesName(version, true);
        assertEquals("ponabe", result);
        result = SpeciesLookup.getSpeciesName(version, false);
        assertEquals(p_pygmaeus_abelii, result);
    }

}
