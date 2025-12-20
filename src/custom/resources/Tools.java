/*
 * Copyright (c) 2018 NOMANA-IT and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * @author fblettner
 */
package custom.resources;

import javax.swing.JOptionPane;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.*;
import java.util.Base64;


/**
 *
 * @author fblettner
 */
public class Tools {
   
    
    public static String encodePasswd(String str) {
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        str = new String(encoder.encodeToString(str.getBytes()));
        return str;
    }

    public static String decodePasswd(String str) throws IOException {
        str = new String(Base64.getDecoder().decode(str));       
        return str;
    }
    
    public static void infoBox(String infoMessage, String titleBar)
    {
        JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
    }


    // Récupérer la valeur d'un tag XML
    public static String getNodeString(String tagName, Element element) {
        NodeList listNode = element.getElementsByTagName(tagName);
        if (listNode != null && listNode.getLength() > 0) {
            NodeList subList = listNode.item(0).getChildNodes();

            if (subList != null && subList.getLength() > 0) {
                return subList.item(0).getNodeValue();
            }
        }

        return null;
    }
 

    public static void executeGS(String inputGS) {

        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(inputGS);
            proc.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}
