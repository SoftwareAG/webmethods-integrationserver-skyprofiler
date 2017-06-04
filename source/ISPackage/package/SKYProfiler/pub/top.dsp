<html>
	<head>
	  <meta http-equiv="Pragma" content="no-cache" />
	  <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />
	  <meta http-equiv="Expires" content="-1" />

	  <link rel="stylesheet" type="text/css" href="css/top.css" />
	  <link rel="stylesheet" type="text/css" href="css/webMethods.css" />
	  
	  <script src="csrf-guard.js.txt"></script>
	  <script>
		function launchHelp() {
		  var url="/WmRoot/doc/OnlineHelp/index.html#context/is-onlinehelp/IS_Server_SrvrStatsScrn";
		  window.open(url, 'help', "directories=no,location=yes,menubar=yes,scrollbars=yes,status=yes,toolbar=yes,resizable=yes", true);
		}

		function logIEout() {
		  if (confirm("OK to log off?")) {
			return true;
		  }
		  else {
			return false;
		  }
		} 

		function loadPage(url) {
		  if (is_csrf_guard_enabled && needToInsertToken) {
			if (url.indexOf("?") != -1){
			  url = url+"&"+ _csrfTokenNm_ + "=" + _csrfTokenVal_;
			}
			else {
			  url = url+"?"+ _csrfTokenNm_ + "=" + _csrfTokenVal_;
			}
		  } 
		  window.location.replace(url);
		}

		function switchToQuiesceMode(mode) {
		  var link = document.getElementById("Qlink");
		  var delayTime = -1;
		  if (mode == "false" || mode == false) {
			delayTime = prompt("OK to enter quiesce mode?\nSpecify the maximum number of minutes to wait before disabling packages:",0);
			if (delayTime == null) { 
			  return false;
			}
			else {
			  if (((parseFloat(delayTime) == parseInt(delayTime)) && !isNaN(delayTime)) && parseInt(delayTime) >= 0) {
				link.href = "quiesce-report.dsp?isQuiesceMode=true&timeout=" + delayTime;
				return true;
			  }
			  else {
				alert("Enter positive integer value.");
				return false;
			  }
			}
		  }
		  if (mode == "true" || mode == true) {
			if (confirm("OK to exit quiesce mode?")) {
			  link.href = "quiesce-report.dsp?isQuiesceMode=false";
			  return true;
			}
			else {
			  return false;
			}
		  }
		  return false;
		}

		function displayMode(mode) {
		  var temp = document.getElementById("quiesceModeMessage");
		  if (temp == null || temp == undefined) 
			return;

		  if (mode == "true" || mode == true) {
			if (temp.innerHTML == '' || temp.innerHTML == '&nbsp;'){
			  temp.style.display = "block";
			  temp.innerHTML = "<center>Integration Server is running in quiesce mode.</center>";
			}    
		  }
		}

		function displayMessage(mode, message) {
		  var temp = document.getElementById("quiesceModeMessage");
		  if (temp == null || temp == undefined) 
			return;
		  if (mode == "true" || mode == true) {
			temp.style.display = "block";
			temp.innerHTML = "<center>"+message+"</center>";
		  }
		  else {
			temp.innerHTML = "";
			temp.style.display = "none";
		  }
		}    

		%ifvar message%
		%ifvar norefresh%
		%else%
		setTimeout("loadPage('top.dsp')", 30000);
		%endif%
		%endif%
	  </script>
	</head>

	<body class="topbar" topmargin="0" leftmargin="0" marginwidth="0" marginheight="0">
	  <table border=0 cellspacing=0 cellpadding=0 height=10 width="100%">
		<tr>
		  <td>
			<table height=14 width="100%" cellspacing=0 cellpadding=0 border=0>
			  <tr>
				<td>
				  <img src="images/is_logo.png" class="saglogo" style="margin:0px 0px 0px 0px; padding:16px 15px 0px 30px; float:left;" />
				  <img src="images/sky-logo.jpg" style="float:left" />
				</td>

				<td nowrap class="topmenu" width="25%">
				  <a target='body' onclick="launchHelp();return false;" href='#'>Help</a>
				</td>
			  </tr>
			</table>
		  </td>
		</tr>
	  </table>
	</body>
</html>
