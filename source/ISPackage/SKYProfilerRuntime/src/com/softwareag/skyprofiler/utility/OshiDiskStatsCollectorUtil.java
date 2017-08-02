/*
 * Copyright 2017 Software AG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.softwareag.skyprofiler.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.WTypes.BSTR;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.windows.Ole32;
import oshi.jna.platform.windows.COM.EnumWbemClassObject;
import oshi.jna.platform.windows.COM.WbemClassObject;
import oshi.jna.platform.windows.COM.WbemLocator;
import oshi.jna.platform.windows.COM.WbemServices;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;

/**
 * The implementation of this with reference with
 * oshi.util.platform.windows.WmiUtil class. This class provides only the
 * required APIs to connect to the windows WMI server, query the data using
 * OSHI's implementation and close the connection. This implementation avoids
 * initializing COM every time to read disk data from server.
 * 
 * @author YEJ
 */
public class OshiDiskStatsCollectorUtil {
	private static boolean comInitialized = false;

	private static boolean securityInitialized = false;

	public static void init() {
		if (!initCOM()) {
			unInitCOM();
		}
	}

	/**
	 * This method can be used to get the required value from the WMI. However,
	 * currently it implemented to return only AvgDiskSecPerTransfer which is
	 * required to show disk latency.
	 * 
	 * @return
	 */
	public static long get() {
		String properties = "AvgDiskSecPerTransfer";

		ValueType[] propertyTypes = new ValueType[] { ValueType.UINT32 };

		// Set up empty map
		Map<String, List<Object>> values = new HashMap<String, List<Object>>();
		String[] props = properties.split(",");
		values.put(properties, new ArrayList<Object>());

		Map<String, List<Object>> result = queryWMI(WmiUtil.DEFAULT_NAMESPACE,
				"Win32_PerfRawData_PerfDisk_PhysicalDisk", properties, "where Name=\"_Total\"", propertyTypes, values,
				props);

		return (Long) result.get("AvgDiskSecPerTransfer").get(0);
	}

	public static void close() {
		unInitCOM();
	}

	private static Map<String, List<Object>> queryWMI(String namespace, String wmiClass, String properties,
			String whereClause, ValueType[] propertyTypes, Map<String, List<Object>> values, String[] props) {

		PointerByReference pSvc = new PointerByReference();
		if (!connectServer(namespace, pSvc)) {
			unInitCOM();
			return values;
		}
		WbemServices svc = new WbemServices(pSvc.getValue());

		PointerByReference pEnumerator = new PointerByReference();
		if (!selectProperties(svc, pEnumerator, properties, wmiClass, whereClause)) {
			svc.Release();
			unInitCOM();
			return values;
		}
		EnumWbemClassObject enumerator = new EnumWbemClassObject(pEnumerator.getValue());

		enumerateProperties(values, enumerator, props, propertyTypes, svc);

		// Cleanup
		enumerator.Release();
		svc.Release();
		// unInitCOM();
		return values;
	}

	private static boolean initCOM() {
		// Step 1: --------------------------------------------------
		// Initialize COM. ------------------------------------------
		HRESULT hres = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
		if (COMUtils.FAILED(hres)) {
			if (hres.intValue() == Ole32.RPC_E_CHANGED_MODE) {
				securityInitialized = true;
				return true;
			}
			return false;
		}
		comInitialized = true;
		if (securityInitialized) {
			// Only run CoInitializeSecuirty once
			return true;
		}
		// Step 2: --------------------------------------------------
		// Set general COM security levels --------------------------
		hres = Ole32.INSTANCE.CoInitializeSecurity(null, new NativeLong(-1), null, null,
				Ole32.RPC_C_AUTHN_LEVEL_DEFAULT, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE, null);
		if (COMUtils.FAILED(hres) && hres.intValue() != Ole32.RPC_E_TOO_LATE) {
			Ole32.INSTANCE.CoUninitialize();
			return false;
		}
		securityInitialized = true;
		return true;
	}

	private static boolean connectServer(String namespace, PointerByReference pSvc) {
		// Step 3: ---------------------------------------------------
		// Obtain the initial locator to WMI -------------------------
		WbemLocator loc = WbemLocator.create();
		if (loc == null) {
			return false;
		}
		// Step 4: -----------------------------------------------------
		// Connect to WMI through the IWbemLocator::ConnectServer method
		// Connect to the namespace with the current user and obtain pointer
		// pSvc to make IWbemServices calls.
		HRESULT hres = loc.ConnectServer(new BSTR(namespace), null, null, null, null, null, null, pSvc);
		if (COMUtils.FAILED(hres)) {
			loc.Release();
			unInitCOM();
			return false;
		}
		loc.Release();

		// Step 5: --------------------------------------------------
		// Set security levels on the proxy -------------------------
		hres = Ole32.INSTANCE.CoSetProxyBlanket(pSvc.getValue(), Ole32.RPC_C_AUTHN_WINNT, Ole32.RPC_C_AUTHZ_NONE, null,
				Ole32.RPC_C_AUTHN_LEVEL_CALL, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE);
		if (COMUtils.FAILED(hres)) {
			new WbemServices(pSvc.getValue()).Release();
			unInitCOM();
			return false;
		}
		return true;
	}

	private static boolean selectProperties(WbemServices svc, PointerByReference pEnumerator, String properties,
			String wmiClass, String whereClause) {
		// Step 6: --------------------------------------------------
		// Use the IWbemServices pointer to make requests of WMI ----
		String query = String.format("SELECT %s FROM %s %s", properties, wmiClass,
				whereClause != null ? whereClause : "");
		HRESULT hres = svc.ExecQuery(new BSTR("WQL"), new BSTR(query),
				new NativeLong(
						EnumWbemClassObject.WBEM_FLAG_FORWARD_ONLY | EnumWbemClassObject.WBEM_FLAG_RETURN_IMMEDIATELY),
				null, pEnumerator);
		if (COMUtils.FAILED(hres)) {
			svc.Release();
			unInitCOM();
			return false;
		}
		return true;
	}

	private static void enumerateProperties(Map<String, List<Object>> values, EnumWbemClassObject enumerator,
			String[] properties, ValueType[] propertyTypes, WbemServices svc) {
		if (propertyTypes.length > 1 && properties.length != propertyTypes.length) {
			throw new IllegalArgumentException("Property type array size must be 1 or equal to properties array size.");
		}
		// Step 7: -------------------------------------------------
		// Get the data from the query in step 6 -------------------
		PointerByReference pclsObj = new PointerByReference();
		LongByReference uReturn = new LongByReference(0L);
		while (enumerator.getPointer() != Pointer.NULL) {
			HRESULT hres = enumerator.Next(new NativeLong(EnumWbemClassObject.WBEM_INFINITE), new NativeLong(1),
					pclsObj, uReturn);
			// Requested 1; if 0 objects returned, we're done
			if (0L == uReturn.getValue() || COMUtils.FAILED(hres)) {
				// Enumerator will be released by calling method so no need to
				// release it here.
				return;
			}
			VARIANT.ByReference vtProp = new VARIANT.ByReference();

			// Get the value of the properties
			WbemClassObject clsObj = new WbemClassObject(pclsObj.getValue());
			for (int p = 0; p < properties.length; p++) {
				String property = properties[p];
				hres = clsObj.Get(new BSTR(property), new NativeLong(0L), vtProp, null, null);

				ValueType propertyType = propertyTypes.length > 1 ? propertyTypes[p] : propertyTypes[0];
				switch (propertyType) {
				case STRING:
					values.get(property).add(vtProp.getValue() == null ? "unknown" : vtProp.stringValue());
					break;
				// uint16 == VT_I4, a 32-bit number
				case UINT16:
					values.get(property).add(vtProp.getValue() == null ? 0L : vtProp.intValue());
					break;
				// WMI Uint32s will return as longs
				case UINT32:
					values.get(property).add(vtProp.getValue() == null ? 0L : vtProp.longValue());
					break;
				// WMI Longs will return as strings so we have the option of
				// calling a string and parsing later, or calling UINT64 and
				// letting this method do the parsing
				case UINT64:
					values.get(property).add(
							vtProp.getValue() == null ? 0L : ParseUtil.parseLongOrDefault(vtProp.stringValue(), 0L));
					break;
				case FLOAT:
					values.get(property).add(vtProp.getValue() == null ? 0f : vtProp.floatValue());
					break;
				case DATETIME:
					// Read a string in format 20160513072950.782000-420 and
					// parse to a long representing ms since eopch
					values.get(property)
							.add(vtProp.getValue() == null ? 0L : ParseUtil.cimDateTimeToMillis(vtProp.stringValue()));
					break;
				case BOOLEAN:
					values.get(property).add(vtProp.getValue() == null ? 0L : vtProp.booleanValue());
					break;
				case PROCESS_GETOWNER:
					// Win32_Process object GetOwner method
					String owner = String.join("\\",
							execMethod(svc, vtProp.stringValue(), "GetOwner", "Domain", "User"));
					values.get(propertyType.name()).add("\\".equals(owner) ? "N/A" : owner);
					break;
				case PROCESS_GETOWNERSID:
					// Win32_Process object GetOwnerSid method
					String[] ownerSid = execMethod(svc, vtProp.stringValue(), "GetOwnerSid", "Sid");
					values.get(propertyType.name()).add(ownerSid.length < 1 ? "" : ownerSid[0]);
					break;
				default:
					// Should never get here! If you get this exception you've
					// added something to the enum without adding it here. Tsk.
					throw new IllegalArgumentException("Unimplemented enum type: " + propertyType.toString());
				}
				OleAuto.INSTANCE.VariantClear(vtProp);
			}

			clsObj.Release();
		}
	}

	private static String[] execMethod(WbemServices svc, String clsObj, String method, String... properties) {
		List<String> result = new ArrayList<String>();
		PointerByReference ppOutParams = new PointerByReference();
		HRESULT hres = svc.ExecMethod(new BSTR(clsObj), new BSTR(method), new NativeLong(0L), null, null, ppOutParams,
				null);
		if (COMUtils.FAILED(hres)) {
			return new String[0];
		}
		WbemClassObject obj = new WbemClassObject(ppOutParams.getValue());
		VARIANT.ByReference vtProp = new VARIANT.ByReference();
		for (String prop : properties) {
			hres = obj.Get(new BSTR(prop), new NativeLong(0L), vtProp, null, null);
			if (!COMUtils.FAILED(hres)) {
				result.add(vtProp.getValue() == null ? "" : vtProp.stringValue());
			}
		}
		obj.Release();
		return result.toArray(new String[result.size()]);
	}

	private static void unInitCOM() {
		if (comInitialized) {
			Ole32.INSTANCE.CoUninitialize();
			comInitialized = false;
		}
	}
}
