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
package org.openfaces.demo.services;

import org.apache.commons.digester.Digester;
import org.openfaces.util.FacesUtil;
import org.openfaces.util.Log;
import org.xml.sax.SAXException;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuService implements Serializable {
    private List<MenuItem> menus;

    public MenuService() {
        loadMenu();
    }

    private void loadMenu() {
        ExternalContext exContext = FacesContext.getCurrentInstance().getExternalContext();
        Digester digester = new Digester();
        digester.setValidating(false);

        digester.addObjectCreate("menu", ArrayList.class);
        digester.addObjectCreate("menu/menuItem", MenuItem.class);
        digester.addObjectCreate("menu/menuItem/pagePatterns", ArrayList.class);
        digester.addObjectCreate("menu/menuItem/keyFeatures", ArrayList.class);
        digester.addObjectCreate("menu/menuItem/demos", ArrayList.class);
        digester.addObjectCreate("menu/menuItem/demos/demoItem", DemoItem.class);

        digester.addSetNext("menu/menuItem", "add");
        digester.addSetNext("menu/menuItem/keyFeatures", "setFeatures");
        digester.addSetNext("menu/menuItem/demos", "setDemos");
        digester.addSetNext("menu/menuItem/demos/demoItem", "add");
        digester.addSetNext("menu/menuItem/pagePatterns/pattern", "add");

        digester.addCallMethod("*/menuItem/name", "setMenuName", 0);
        digester.addCallMethod("*/menuItem/componentName", "setComponentName", 0);
        digester.addCallMethod("*/menuItem/url", "setMenuUrl", 0);
        digester.addCallMethod("*/menuItem/image", "setMenuImage", 0);
        digester.addCallMethod("*/menuItem/selectedImage", "setSelectedMenuImage", 0);
        digester.addCallMethod("*/keyFeatures/feature", "add", 0);
        digester.addCallMethod("*/demos/demoItem/demoName", "setDemoName", 0);
        digester.addCallMethod("*/demos/demoItem/demoUrl", "setDemoUrl", 0);

        try {
            menus = (List<MenuItem>) digester.parse(exContext.getResource("/WEB-INF/menu.xml").openStream());
        } catch (IOException e) {
            Log.log(e.getMessage(), e);
        } catch (SAXException e) {
            Log.log(e.getMessage(), e);
        }
    }

    public List getMenus() {
        getSelectedMenu();
        return menus;
    }

    public MenuItem getSelectedMenu() {
        FacesContext facesCotnext = FacesContext.getCurrentInstance();
        Map requestMap = facesCotnext.getExternalContext().getRequestMap();
        String key = MenuService.class.getName() + ".selectedMenu";
        MenuItem selectedMenu = (MenuItem) requestMap.get(key);
        if (selectedMenu == null) {
            selectedMenu = getSelectedMenu_internal();
            requestMap.put(key, selectedMenu);
        }
        return selectedMenu;
    }

    public void selectionChanged(ActionEvent e) {
        MenuItem selectedMenu = getSelectedMenu();
        int newIndex = Integer.valueOf(FacesUtil.getRequestParameterMapValue("menuItemIndex").toString());  
        DemoItem di = selectedMenu.getDemos().get(newIndex);
        selectedMenu.setSelectedDemo(di);
        selectedMenu.setSelectedTabIndex(newIndex);
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        try {
            externalContext.redirect(externalContext.getRequestContextPath() + di.getDemoUrl());
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        } finally {
            context.responseComplete();
        }
    }

    private MenuItem getSelectedMenu_internal() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        String requestURL = normalizeMenuUrl(request.getRequestURL().toString());
        MenuItem selectedMenu = null;
        menusIteration:
        for (MenuItem menuItem : menus) {
            menuItem.setSelected(false);
            List demos = menuItem.getDemos();
            if (demos != null && demos.size() > 0) {
                for (int i = 0, count = demos.size(); i < count; i++) {
                    DemoItem di = (DemoItem) demos.get(i);
                    String demoUrl = normalizeMenuUrl(di.getDemoUrl());
                    if (requestURL.endsWith(demoUrl) && demoUrl.length() > 0) {
                        selectedMenu = menuItem;
                        menuItem.setSelected(true);
                        menuItem.setSelectedTabIndex(i);
                        continue menusIteration;
                    }
                }
            } else {
                String menuUrl = normalizeMenuUrl(menuItem.getMenuUrl());
                if (requestURL.endsWith(menuUrl) && menuUrl.length() > 0) {
                    selectedMenu = menuItem;
                    menuItem.setSelected(true);
                }
            }
        }
        return selectedMenu;
    }

    private String normalizeMenuUrl(String menuUrl) {
        if (menuUrl.endsWith(".jsp") || menuUrl.endsWith(".jsf")) {
            menuUrl = menuUrl.substring(0, menuUrl.length() - 4);
        }
        return menuUrl;
    }

}