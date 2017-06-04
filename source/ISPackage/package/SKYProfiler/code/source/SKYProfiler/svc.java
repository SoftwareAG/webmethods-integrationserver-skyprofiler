package SKYProfiler;

// -----( IS Java Code Template v1.2
// -----( CREATED: 2017-05-26 14:25:50 IST
// -----( ON-HOST: MCYEJ01.eur.ad.sag

import com.wm.data.*;
import com.wm.util.Values;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
// --- <<IS-START-IMPORTS>> ---
import com.wm.util.sort.QuickSort;
import com.wm.util.sort.Sortable;
import com.wm.app.b2b.server.Package;
import com.wm.app.b2b.server.PackageManager;
import com.wm.app.b2b.server.invoke.InvokeManager;
import com.wm.util.List;
import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import com.softwareag.skyprofiler.*;
import com.softwareag.skyprofiler.data.*;
import com.softwareag.skyprofiler.utility.*;
import com.sun.management.OperatingSystemMXBean;
import com.google.gson.Gson;
// --- <<IS-END-IMPORTS>> ---

public final class svc

{
	// ---( internal utility methods )---

	final static svc _instance = new svc();

	static svc _newInstance() { return new svc(); }

	static svc _cast(Object o) { return (svc)o; }

	// ---( server methods )---




	public static final void getConfiguration (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(getConfiguration)>> ---
		// @sigtype java 3.5
		Map<String, Object> exludedPkgsMap = SkyProfilerManager.getInstance().getIncludedPackageNameMap();
		
		Package [] packages  = PackageManager.getAllPackages();
		Values  [] packagesV = new Values[packages.length];
		
		int offset = 0;
		
		String packageName;
		String checked;
		for (int i=0; i<packages.length; i++) {
			if (packages[i] != null) {
				packageName=packages[i].getName();
				checked=exludedPkgsMap.containsKey(packageName) ? "selected" : "";
				
				Object [][] o = {
		                    { "name", packageName},
		                    { "selected", checked}
				};
				
				packagesV[offset] = new Values(o) {
		    		public int compare(Sortable compareTo, boolean reverse, int column) {
		    			if (compareTo instanceof Values) {
		    				String skey = getSortKeyValue();
		    				String ctskey = ((Values)compareTo).getSortKeyValue();
		    				if (skey != null && ctskey != null)
		    					return skey.compareToIgnoreCase(ctskey);
		    			}
		    			return 0;
		    		}
		    	};
		    	
		    	offset++;
			}
		}
		
		Properties props = SkyProfilerManager.getInstance().getConfigurationProperties();
		
		IDataCursor pipelineCursor=pipeline.getCursor();
		IDataUtil.put(pipelineCursor, "kafkaBootstrapUrl", props.getProperty(Utils.KAFKA_BOOTSTRAP_URL_ATTR));
		IDataUtil.put(pipelineCursor, "kafkaTopicName", props.getProperty(Utils.KAFKA_TOPIC_NAME_ATTR));
		IDataUtil.put(pipelineCursor, "externalHostname", props.getProperty(Utils.EXTERNAL_HOSTNAME_ATTR));
		IDataUtil.put(pipelineCursor, "packages", QuickSort.sort(packagesV));
		pipelineCursor.destroy();
		// --- <<IS-END>> ---

                
	}



	public static final void isRunning (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(isRunning)>> ---
		// @sigtype java 3.5
		// [o] field:0:required status
		IDataCursor pipelineCursor = pipeline.getCursor();
		IDataUtil.put(pipelineCursor, "status", SkyProfilerManager.getInstance().isRunning());
		pipelineCursor.destroy();
		// --- <<IS-END>> ---

                
	}



	public static final void start (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(start)>> ---
		// @sigtype java 3.5
		SkyProfilerManager.getInstance().startProfiler();
		// --- <<IS-END>> ---

                
	}



	public static final void stop (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(stop)>> ---
		// @sigtype java 3.5
		SkyProfilerManager.getInstance().stopProfiler();
		// --- <<IS-END>> ---

                
	}



	public static final void updateSettings (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(updateSettings)>> ---
		// @sigtype java 3.5
		// [i] field:0:required includedPackages
		// [i] field:0:required kafkaBootstrapUrl
		// [i] field:0:required kafkaTopicName
		// [i] field:0:required externalHostname
		// [o] field:0:required message
		IDataCursor pipelineCursor = pipeline.getCursor();
		String	includedPackages = IDataUtil.getString( pipelineCursor, "includedPackages" );
		String	kafkaBootstrapUrl = IDataUtil.getString( pipelineCursor, "kafkaBootstrapUrl" );
		String	kafkaTopicName = IDataUtil.getString( pipelineCursor, "kafkaTopicName" );
		String	externalHostname  = IDataUtil.getString( pipelineCursor, "externalHostname" );
		
		try {
			SkyProfilerManager.getInstance().updateConfig(includedPackages, kafkaBootstrapUrl, kafkaTopicName, externalHostname);
			IDataUtil.put( pipelineCursor, "message", "Configuration saved successfully.");
		} catch(Exception ex) {
			throw new ServiceException("Failed to update the configuration.");
		}
		
		pipelineCursor.destroy();
		// --- <<IS-END>> ---

                
	}
}

