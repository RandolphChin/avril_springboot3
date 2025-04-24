package com.randy.chin.util;

import java.io.InputStream;

import com.aspose.cells.License;

public class CellsUtil {
    public static void authrolizeLicense() {
        try {
            InputStream is = License.class.getResourceAsStream("/com.aspose.cells.lic_2999.xml");
            License asposeLicense = new License();
            asposeLicense.setLicense(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
