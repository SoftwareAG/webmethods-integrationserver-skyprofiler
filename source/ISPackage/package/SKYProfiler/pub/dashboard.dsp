<!doctype html>
<html>
    <head>
        <meta http-equiv="Pragma" content="no-cache" />
        <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />
        <meta http-equiv="Expires" content="-1" />

        <title>SKYProfiler</title>

        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.4/css/bootstrap.min.css" />
		<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/angular-ui-notification/0.2.0/angular-ui-notification.min.css" />
        <link rel="stylesheet" href="css/lib/nya-bs-select.min.css" />
		<link rel="stylesheet" href="css/skyprofiler.css" />

        <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.0/jquery.min.js"></script>
		<script src="https://cdnjs.cloudflare.com/ajax/libs/angular.js/1.5.8/angular.min.js"></script>
		<script src="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.4/js/bootstrap.min.js"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/angular-ui-bootstrap/0.13.3/ui-bootstrap-tpls.js"></script>
		<script src="https://cdnjs.cloudflare.com/ajax/libs/angular-ui-notification/0.2.0/angular-ui-notification.min.js"></script>
        <script src="js/lib/nya-bs-select.min.js"></script>
		<script src="js/skyprofiler.js"></script>
    </head>

    <body ng-app="skyProfilerApp" ng-controller="skyProfilerCtrl" data-ng-init="init()" ng-cloak="">
        <div class="vertical-center">
            <div class="container text-center">
                <div class="skyprofile-title">
                    <span class="welcome">Welcome to </span> SKY Profiler
                </div>

                <p class="intro-text">
                SKY Profiler monitors Integration Server's service invocations. It tracks the invocations and the monitored data is seen by Clients in real time. This helps users track the time each service invocation takes and further drills down to the child service to identify which service contributes to time.
                </p>

                <p>
                    <button id="start" ng-if="!isRunning" title="To get started quickly click on this button. Profiling will be started with default configuration." type="button" class="btn btn-lg btn-primary" ng-click="doStart(null, $event)"><i class="glyphicon glyphicon-play"></i> Start</button>
                    <button id="stop" ng-if="isRunning" title="SKY Profiler is already running." type="button" class="btn btn-lg btn-success" ng-click="doStop(null, $event)"><i class="glyphicon glyphicon-stop"></i> Stop</button>
                    <button id="configuration" title="To configure the profiler before starting the profiling click on this button." type="button" class="btn btn-lg btn-default" ng-click="openConfigModal()"><i class="glyphicon glyphicon-cog"></i> Configure</button>
                </p>
            </div>
        </div>

        <!-- Configuration Modal -->
        <script type="text/ng-template" id="configModal.html">
            <div class="modal-header">
                <h4 class="modal-title">Configurations</h4>
            </div>

            <div class="modal-body" >
                <form class="form-horizontal" role="form">
                    <div class="form-group">
                        <label for="includedPkgList" class="col-sm-6 control-label">Include Packages for Profiling</label>

                        <div class="col-sm-6">
                            <ol id="dynamic-options" title="Select all packages whose service execution needs to be monitored" class="form-control nya-bs-select" ng-model="selectedPackageList" data-live-search="true" multiple>
                                <li nya-bs-option="packageInfo in configurationData.packages" deep-watch="true" value="packageInfo.name">
                                    <a>
                                        {{ packageInfo.name }}
                                        <span class="glyphicon glyphicon-ok check-mark"></span>
                                    </a>
                                </li>
                            </ol>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="kafkaBootstrapUrl" class="col-sm-6 control-label">Kafka Bootstrap Url</label>

                        <div class="col-sm-6">
                            <input id="kafkaBootstrapUrl" type="text" class="form-control" placeholder="<hostname>:<portnumber>" title="This should be kafka bootstrap server URL. Ex - localhost:9092" ng-model="configurationData.kafkaBootstrapUrl">
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="kafkaTopicName" class="col-sm-6 control-label">Kafka Topic Name</label>

                        <div class="col-sm-6">
                            <input id="kafkaTopicName" type="text" class="form-control" placeholder="<kafkatopicname>" title="Provide the topic name to which service execution events needs to be published.&#013;This should be same as server name provided in the UI and it is case sensitive." ng-model="configurationData.kafkaTopicName">
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="externalHostname" class="col-sm-6 control-label">External Server Hostname</label>

                        <div class="col-sm-6">
                            <input id="externalHostname" type="text" class="form-control" placeholder="<hostname>" title="It is the server to which IS will be communicating when the service is executed.&#013;This will help to find out bottleneck due to network latency.&#013;If no external server is present, leave this blank." ng-model="configurationData.externalHostname">
                        </div>
                    </div>
                </form>
            </div>

            <div class="modal-footer">
                <button id="ConfigSave" type="button" class="btn btn-primary" ng-click="saveConfig()" data-dismiss="modal"><i class="glyphicon glyphicon-ok"></i> Save</button>
                <button id="ConfigCancel" type="button" class="btn btn-default" ng-click="cancel()" data-dismiss="modal"><i class="glyphicon glyphicon-remove"></i> Cancel</button>
            </div>
        </script>
    </body>
</html>
