%invoke wm.server.ui:mainMenu%

<html>
	<head>
	  <title>SKYProfiler - webMethods Integration Server</title>
	  
	  <link rel="stylesheet" href="css/layout.css" type="text/css" />
	  <link rel="icon" href="/WmRoot/favicon.ico" />
	</head>
	
	<body>
		<div>
			<iframe class="top" name="header" src="top.dsp" id="top"></iframe>
		</div>
		<div class="bottom">
			<iframe name="body" id="body" src="dashboard.dsp""></iframe>
		</div>
	</body>
</html>

%onerror%

<html>
  <head>
    <title>Access Denied</title>
  </head>
  <body>
    Access Denied.
    <br>
    <br>
    Services necessary to show the Integration Server Administrator are currently unavailable on this 
    port.  This is most likely due to port security restrictions.
    <br>
    <br>
    If this is the only port available to access the Integration Server, contact webMethods Support.
  </body>
</html>

%endinvoke%
