<%-- 
    Document   : zidan
    Created on : Jun 25, 2015, 3:28:49 PM
    Author     : Jergi
--%>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<title>Vývoj platov</title>
	<link rel="stylesheet" href="css/bootstrap.min.css">
	<style>
	h1{background-color:black;
		font-family:arial;
		color:white;
		padding: 5px;
		margin-top: 0px;
		height: 50px;
	}
	body { margin:0px; }
	p { font-family:georgia; }
	.div1{
		display: inline-block;
		position: relative;
		left: 20px;
	}
	.div2{
		display: inline-block;
		position: absolute;
		left: 220px;
	}
	.div3{
		display: inline-block;
		position: absolute;
		left: 450px;
	}
	.div4{
		display: inline-block;
		position: absolute;
		left: 620px;
	}
	.div5{
		display: inline-block;
		position: absolute;
		left: 750px;
	}
	.div6{
		display: inline-block;
		position: absolute;
		left: 1100px;
		width: 140px;
		height: 283px;
		background-color: #F0F0F0;
	}
	</style>
</head>


<body>
<h1>Vývoj platov v ČR a inde</h1>
<div class="div1">
  <p>Zdroj</p>
  <select id="zdroj" width="100" style="width: 100px" onchange="zdrojChanged()">
    <option value="eurostat">Eurostat</option>
    <option value="cso">ČSÚ</option>
  </select>
  <p style="margin-top:40px">Porovanie</p>
  <select id="porovnanie1" width="100" style="width: 150px" onchange="porovnanie1changed()"></select>
  <p style="margin-top:90px">Porovanie</p>
  <select id="porovnanie2" width="100" style="width: 150px" onchange="porovnanie2changed()"></select>
</div>

<div class="div2">
  <p style="margin-top:85px">Parameter</p>
  <input type="text" id="parameter1"></input>
  <p style="margin-top:87px">Parameter</p>
  <input type="text" id="parameter2"></input>
</div>

<div class="div3">
  <select id="hodnoty1" width="100" style="width: 150px; margin-top:20px" size="7">
    
  </select> <br><br>
  <select id="hodnoty2" width="100" style="width: 150px" size="7">
    
  </select>
</div>

<div class="div4">
  <button onclick="pridat1()" style="margin-top:20px; width:80px">Pridať</button> <br>
  <button onclick="odstranit1()" style="margin-top:20px; width:80px">Odobrať</button> <br>
  <button onclick="pridat2()" style="margin-top:70px; width:80px">Pridať</button> <br>
  <button onclick="odstranit2()" style="margin-top:20px; width:80px">Odobrať</button> <br>
</div>

<div class="div5">
  <p style="margin-top:14px">Obmedzenie</p>
  <select id="obmedzenieAtr" style="width: 150px"></select> <br><br>
  <p style="display: inline-block;">Parameter</p>
  <button onclick="pridat3()" style="margin-left:150px; width:80px">Pridať</button><br>
  <input type="text" id="obmedzenieTxt" style="width:145px"></input>
  <button onclick="odstranit3()" style="margin-left:71px; width:80px">Odobrať</button><br>

  <select id="hodnoty3" width="100" style="width: 300px; margin-top:20px" size="7"></select>
</div>

<div class="div6">
<div style="left: 20px; position: absolute;">
  <br>
  <p>Obdobie</p>
  <input type="radio" id="radioR" name="obdobie" value="r" checked="checked">Ročne<br>
  <input type="radio" id="radioQ" name="obdobie" value="q">Štvrťročne<br>
  <form method="post" action="${pageContext.request.contextPath}/compare">
  	<input type="hidden" name="comparision" id="comp"/>
        <input type="hidden" name="comparision2" id="comp2"/>
        <input type="hidden" name="filter" id="compFilter"/>
        <input type="hidden" name="time" id="compTime"/>
        <br/><br/><br/><br/><br/><br/><br/>
  	<input onclick="porovnaj()" type="submit" value="Porovnaj" style="width:100px;height:40px;"/>
  </form>
</div>
</div>

<br><br>
<hr>
<c:if test="${not empty picture}">
    <img src="${picture}" alt="obrazok"/>
</c:if>

<script type="text/javascript">
	hodnoty1count = 0;
	hodnoty2count = 0;
	obmedzeniaCount = 0;
	eurostat = [];
	cso = [];
	currentSource = "eurostat";
	porovnanie1index = 0;
	porovnanie2index = 0;
	atributyEurostat = [{en:"geo", cz:"Krajina"},{en:"currency", cz:"Mena"},{en:"worktime", cz:"Pracovný čas"},{en:"sex", cz:"Pohlavie"},{en:"sizeclas", cz:"Trieda podniku"},{en:"nace_r2", cz:"Klasifikácia podľa NACE 2"},{en:"isco88", cz:"Klasifikácia podľa ISCO88"},{en:"estruct", cz:"Typ príjmu"},{en:"ecase", cz:"Medián"}];

	atributyCSU = [{en:"currency", cz:"Mena"},{en:"sector", cz:"Sektor"},{en:"cz_nace", cz:"Klasifikácia podľa CZ-NACE"},{en:"region", cz:"Kraj"},{en:"sex", cz:"Pohlavie"},{en:"main_kzam_class", cz:"Klasifikácia odvetvia (KZAM)"}];

	eurostatAtributes();


	function pridat1(){
		var param = document.getElementById("parameter1").value;
		if(param.length > 0){
    		AddOpt = new Option(param, param);
    		document.getElementById("hodnoty1").options[hodnoty1count++] = AddOpt;

    	}
	}
	function odstranit1(){
		var index = document.getElementById("hodnoty1").selectedIndex;
		if(index >= 0){
			document.getElementById("hodnoty1").options[index] = null;
			hodnoty1count--;
		}
	}
	function pridat2(){
		if(document.getElementById("porovnanie2").selectedIndex == 0) alert("Nie je možné pridať parameter k N/A - nedefinované");
		else{
			var param = document.getElementById("parameter2").value;
	    	if(param.length > 0){
	    		AddOpt = new Option(param, param);
	    		document.getElementById("hodnoty2").options[hodnoty2count++] = AddOpt;
			}	
		}
	}
	function odstranit2(){
		var index = document.getElementById("hodnoty2").selectedIndex;
		if(index >= 0){
			document.getElementById("hodnoty2").options[index] = null;
			hodnoty2count--;
		}
	}
	function pridat3(){
		var atribut = document.getElementById("obmedzenieAtr").value;
		var hodnota = document.getElementById("obmedzenieTxt").value;
		var obmedzenieItem = atribut + ":" + hodnota;
		if(atribut.length > 0 && hodnota.length > 0){
			var hodnoty = document.getElementById("hodnoty3").options;
			var alreadyIn = false;
			//kontrola ci tam nahodou uz ten atribut nie je
			for (var i = 0; i < hodnoty.length; i++) {
				str = hodnoty[i].value.split(":")[0];
				if(str == atribut) alreadyIn = true;
			};
			if(alreadyIn) alert("Povolené maximálne 1 dané obmedzenie!");
			else {
    			AddOpt = new Option(obmedzenieItem, obmedzenieItem);
    			document.getElementById("hodnoty3").options[obmedzeniaCount++] = AddOpt;
    		}
		}
	}
	function odstranit3(){
		var index = document.getElementById("hodnoty3").selectedIndex;
		if(index >= 0){
			document.getElementById("hodnoty3").options[index] = null;
			obmedzeniaCount--;
		}
	}
	function porovnanie1changed(){
		if(hodnoty1count > 0){
			var porovnanie1Value = document.getElementById("porovnanie1").options[porovnanie1index].value;
			var dialogResult = confirm("Parametre pre '" + porovnanie1Value + "' nebudú uložené!");
			if(dialogResult){
				document.getElementById("hodnoty1").innerHTML = "";
				hodnoty1count = 0;
				porovnanie1index = document.getElementById("porovnanie1").selectedIndex;
			} else document.getElementById("porovnanie1").selectedIndex = porovnanie1index;
		}
		//document.getElementById("parameter1").value = "";
		porovnanie1index = document.getElementById("porovnanie1").selectedIndex;
	}
	function porovnanie2changed(){
 		if(hodnoty2count > 0){
			var porovnanie2Value = document.getElementById("porovnanie2").options[porovnanie2index].value;
			var dialogResult = confirm("Parametre pre '" + porovnanie2Value + "' nebudú uložené!");
			if(dialogResult){
				document.getElementById("hodnoty2").innerHTML = "";
				hodnoty2count = 0;
				porovnanie2index = document.getElementById("porovnanie2").selectedIndex;
			} else document.getElementById("porovnanie2").selectedIndex = porovnanie2index;
		}
		//document.getElementById("parameter2").value = "";
		porovnanie2index = document.getElementById("porovnanie2").selectedIndex;
	}
	function zdrojChanged(){
		var dialogResult = true;
		if(hodnoty1count > 0 || hodnoty2count > 0 || document.getElementById("hodnoty3").options.length > 0){
			dialogResult = confirm("Parametre nebudú uložené!");
		}
		if(dialogResult){
			clearAtributes();
			if(currentSource == "eurostat"){
				csuAtributes();
				currentSource = "csu";
			} else {
				eurostatAtributes();
				currentSource = "eurostat";
			}
		}
                else {
			if(currentSource == "eurostat") document.getElementById("zdroj").selectedIndex = 0;
			else document.getElementById("zdroj").selectedIndex = 1;
		}
	}
	function eurostatAtributes(){
		for (var i = 0; i < atributyEurostat.length; i++) {
			AddOpt = new Option(atributyEurostat[i].cz, atributyEurostat[i].en);
			document.getElementById("porovnanie1").options[i] = AddOpt;
			AddOpt = new Option(atributyEurostat[i].cz, atributyEurostat[i].en); //treba novy objekt
			document.getElementById("porovnanie2").options[i+1] = AddOpt;
			AddOpt = new Option(atributyEurostat[i].cz, atributyEurostat[i].en);
			document.getElementById("obmedzenieAtr").options[i] = AddOpt;
		};
		addEmptyAtribute();
	}
	function csuAtributes(){
		for (var i = 0; i < atributyCSU.length; i++) {
			AddOpt = new Option(atributyCSU[i].cz, atributyCSU[i].en);
			document.getElementById("porovnanie1").options[i] = AddOpt;
			AddOpt = new Option(atributyCSU[i].cz, atributyCSU[i].en); //treba novy objekt
			document.getElementById("porovnanie2").options[i+1] = AddOpt;
			AddOpt = new Option(atributyCSU[i].cz, atributyCSU[i].en);
			document.getElementById("obmedzenieAtr").options[i] = AddOpt;
		};
		addEmptyAtribute();
	}
	function addEmptyAtribute(){
		AddOpt = new Option("N/A", "empty");
		document.getElementById("porovnanie2").options[0] = AddOpt;
	}
	function clearAtributes(){
		document.getElementById("porovnanie1").innerHTML = "";
		document.getElementById("porovnanie2").innerHTML = "";
		document.getElementById("obmedzenieAtr").innerHTML = "";
		document.getElementById("obmedzenieTxt").value = "";
		document.getElementById("parameter1").value = "";
		document.getElementById("parameter2").value = "";
		document.getElementById("hodnoty1").innerHTML = "";
		document.getElementById("hodnoty2").innerHTML = "";
		document.getElementById("hodnoty3").innerHTML = "";
                hodnoty1count = 0;
                hodnoty2count = 0;
	}
	//sem pojde JAVA 
	function porovnaj(){
		if(hodnoty1count<=0) alert("Nie su zvolene ziadne parametre!");
		else{
			console.log("hocico");
			var comparision1 = [];
			var comparision2 = [];
			var filters = [];
			document.getElementById("comp").value = toComparision1();
                        document.getElementById("comp2").value = toComparision2();
                        document.getElementById("compFilter").value = toFilters();
                        document.getElementById("compTime").value = document.getElementById("radioR").checked ? "r" : "q";
			comparision1 = toComparision1();
			comparision2 = toComparision2(); //prazdne [] ak tam nic nie je
			filters = toFilters();
			console.log("vykonal som sa");

		}
	}

	function toComparision1(){
		var comparision1=[];
		var index = document.getElementById("porovnanie1").selectedIndex;
		var atribut = document.getElementById("porovnanie1").options[index].value;
		comparision1.push(atribut);
		var hodnoty = document.getElementById("hodnoty1").options;
		for (var i = 0; i < hodnoty1count; i++) {
			comparision1.push(hodnoty[i].value);
		};
		return comparision1;
	}

	function toComparision2(){
		var comparision2 = [];
		var index = document.getElementById("porovnanie2").selectedIndex;
		if(hodnoty2count <= 0) return comparision2;
		var atribut = document.getElementById("porovnanie2").options[index].value;
		comparision2.push(atribut);
		var hodnoty = document.getElementById("hodnoty2").options;
		for (var i = 0; i < hodnoty2count; i++) {
			comparision2.push(hodnoty[i].value);
		};
		return comparision2;
	}

	function toFilters(){
		var filters = [];
		if(currentSource == "eurostat") filters.push("source:eurostat");
		else filters.push("source:csu");

		var hodnoty = document.getElementById("hodnoty3").options;
		for (var i = 0; i < hodnoty.length; i++) {
			filters.push(hodnoty[i].value);
		};
		return filters;
	}
</script>

</body>
</html>
