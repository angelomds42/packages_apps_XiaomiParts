/*
 * Copyright (C) 2023-2024 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

 package org.lineageos.settings.utils;

 import android.content.ComponentName;
 import android.content.Context;
 import android.content.Intent;
 import android.content.pm.PackageManager;
 import android.os.UserHandle;
 import androidx.preference.PreferenceManager;
 
 import java.util.HashMap;
 import java.util.Map;
 
 public class ComponentUtils {
 
     private static final Map<String, Boolean> sServiceState = new HashMap<>();
 
    /**
     * Enables or disables a service based on the value of a SharedPreferences.
     *
     * @param context        The context.
     * @param isSupported    Whether the functionality is supported by the device.
     * @param preferenceKey  The key of the SharedPreferences that controls the service.
     * @param serviceClass   The class of the service to control.
     */
     public static void toggleService(Context context, boolean isSupported, String preferenceKey, Class<?> serviceClass) {
         if (!isSupported) {
             return;
         }
 
         boolean shouldBeEnabled = PreferenceManager.getDefaultSharedPreferences(context)
                 .getBoolean(preferenceKey, false);
         boolean isRunning = sServiceState.getOrDefault(preferenceKey, false);
 
         if (shouldBeEnabled && !isRunning) {
             context.startServiceAsUser(new Intent(context, serviceClass), UserHandle.CURRENT);
             sServiceState.put(preferenceKey, true);
         } else if (!shouldBeEnabled && isRunning) {
             context.stopServiceAsUser(new Intent(context, serviceClass), UserHandle.CURRENT);
             sServiceState.put(preferenceKey, false);
         }
     }
 
     /**
      * Enables or disables a specified Android component dynamically at runtime.
      *
      * @param context       The context from which the component will be enabled or disabled.
      * @param componentClass The class of the component to be enabled or disabled.
      * @param enable        If true, the component will be enabled; if false, it will be disabled.
      */
     public static void toggleComponent(Context context, Class<?> componentClass, boolean enable) {
         ComponentName componentName = new ComponentName(context, componentClass);
         PackageManager packageManager = context.getPackageManager();
         int currentState = packageManager.getComponentEnabledSetting(componentName);
         int newState = enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                 PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
 
         if (currentState != newState) {
             packageManager.setComponentEnabledSetting(
                     componentName,
                     newState,
                     PackageManager.DONT_KILL_APP
             );
         }
     }
 }