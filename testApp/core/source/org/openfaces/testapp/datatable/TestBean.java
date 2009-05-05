/*
 * OpenFaces - JSF Component Library 2.0
 * Copyright (C) 2007-2009, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */
package org.openfaces.testapp.datatable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Dmitry Pikhulya
 */
public class TestBean {
    private static int idCounter = 0;
    private String id;
    private String field1;
    private int field2;
    private boolean booleanField;
    private Map customFields = new HashMap();

    private static Map<String, TestBean> idToBeanMap = new HashMap<String, TestBean>();

    public TestBean(String field1, int field2) {
        this.field1 = field1;
        this.field2 = field2;
        id = String.valueOf(idCounter++);
        idToBeanMap.put(id, this);
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            String customFieldName = String.valueOf(i);
            String customFieldValue = String.valueOf(Math.abs(random.nextInt()) % 1000);
            customFields.put(customFieldName, customFieldValue);
        }
    }

    public Map getCustomFields() {
        return customFields;
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public int getField2() {
        return field2;
    }

    public void setField2(int field2) {
        this.field2 = field2;
    }


    public boolean isBooleanField() {
        return booleanField;
    }

    public void setBooleanField(boolean booleanField) {
        this.booleanField = booleanField;
    }

    public String getId() {
        return id;
    }

    public static TestBean findById(String id) {
        return idToBeanMap.get(id);
    }
}