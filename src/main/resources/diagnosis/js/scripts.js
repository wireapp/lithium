/*
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 */

function authenticate() {
  var request = new XMLHttpRequest();
  request.open('POST', '/provider/login', true);
  request.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
  request.withCredentials = true;
  var data = {};
  data.email = document.getElementById("email").value;
  data.password = document.getElementById("password").value;

  request.onload = function() {
    if (request.status >= 200 && request.status < 400) {
      location.href = "/diagnosis/diagnosis.htm";
    } else {
      alert(request.responseText);
    }
  };

  request.onerror = function() {
    alert("Could not reach the backend: " + backend)
  };

  request.send(JSON.stringify(data));
  return false;
}

function diagnosis() {
    let base_url = 'https://' + location.host;

    makeRequest('GET', '/provider/services', true, null).then(function(services_response) {
            let services = JSON.parse(services_response);
            var data = {};
            data.expected_base_url = base_url;
            data.services = services;
            console.log(data);
            makeRequest('POST', '/provider/diagnosis', false, JSON.stringify(data)).then(function(matches_response) {
                    let matches = JSON.parse(matches_response);
                    var diffs = [];
                    for (var i = 0; i < matches.length; i++) {
                        if(matches[i].matching_base_url && matches[i].matching_public_key && matches[i].matching_auth_token) {
                                document.getElementById("diagnosisContainer").style.display = "none";
                                document.getElementById("successContainer").style.display = "block";
                                return;
                        }
                    }
                    document.getElementById("diagnosisContainer").style.display = "none";
                    for (var i = 0; i < matches.length; i++) {
                        addMatchingRow(matches[i], services[i]);
                    }
                    document.getElementById("failedContainer").style.display = "block";
            }).catch(function(err) {
                    alert(err.statusText + ": " + err.response);
            });
    }).catch(function(err) {
            alert(err.statusText + ": " + err.response);
    });
}

function addMatchingRow(match, service) {
    var table_body = document.getElementById('differencesTableBody');
    var tr = document.createElement('tr');
    var td = document.createElement('td');
    var td2 = document.createElement('td');
    var td3 = document.createElement('td');
    var td4 = document.createElement('td');
    var td5 = document.createElement('td');
    td.appendChild(document.createTextNode(match.name));
    var icon1 = match.matching_base_url? "glyphicon-ok":"glyphicon-remove";
    var span1 = document.createElement('span');
    span1.className = "glyphicon " + icon1;
    span1.style.color = match.matching_base_url? "#32CD32":"#FF0000";
    td2.appendChild(span1);
    var icon2 = match.matching_public_key? "glyphicon-ok":"glyphicon-remove";
    var span2 = document.createElement('span');
    span2.className = "glyphicon " + icon2;
    span2.style.color = match.matching_public_key? "#32CD32":"#FF0000";
    td3.appendChild(span2);
    var icon3 = match.matching_auth_token? "glyphicon-ok":"glyphicon-remove";
    var span3 = document.createElement('span');
    span3.className = "glyphicon " + icon3;
    span3.style.color = match.matching_auth_token? "#32CD32":"#FF0000";
    td4.appendChild(span3);
    var button = document.createElement('button');
    button.className = "btn btn-primary";
    button.innerHTML = "Inspect";
    console.log(service);
    button.onclick = function() { inspect(service) };
    td5.appendChild(button);
    tr.appendChild(td);
    tr.appendChild(td2);
    tr.appendChild(td3);
    tr.appendChild(td4);
    tr.appendChild(td5);
    table_body.appendChild(tr);
}

function inspect(service) {
    document.getElementById("serviceName").innerHTML = service.name;
    document.getElementById("serviceId").innerHTML = service.id;
    document.getElementById("serviceBaseUrl").innerHTML = service.base_url;
    document.getElementById("servicePublicKey").value = service.public_keys[0].pem;
    document.getElementById("serviceAuthTokens").innerHTML = "";
    for(var i = 0; i < service.auth_tokens.length; i++) {
        document.getElementById("serviceAuthTokens").innerHTML += service.auth_tokens[i] + "<br/>";
    }
    document.getElementById("inspectContainer").style.display = "block";
}

function makeRequest(method, url, withCredentials, data) {
  return new Promise(function (resolve, reject) {
    var request = new XMLHttpRequest();
    request.open(method, url, true);
    request.setRequestHeader('Accept', 'application/json');
    request.withCredentials = withCredentials;
    request.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');

    request.onload = function () {
      if (request.status >= 200 && request.status < 300) {
        resolve(request.response);
      } else {
        reject({
          status: this.status,
          statusText: request.statusText,
          response: request.response
        });
      }
    };
    request.onerror = function () {
      reject({
        status: this.status,
        statusText: request.statusText,
        response: request.response
      });
    };
    if(method == "POST") {
      request.send(data);
    } else {
      request.send();
    }
  });
}