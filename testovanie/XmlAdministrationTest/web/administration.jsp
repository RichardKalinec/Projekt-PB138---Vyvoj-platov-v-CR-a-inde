<%-- 
    Document   : administration
    Created on : Jun 8, 2015, 7:33:57 PM
    Author     : Marek Jonis
--%>

<%@page import="admin.UploadedFile"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Administration</title>
    </head>
    <body>
        <h1>Administration page!</h1>
        <c:if test="${not empty message}">
            <br/>
            <font style="color: red"><b>
                <c:out value="${message}"/>
            </b></font>
            <br/>
        </c:if>
        <c:choose>
            <c:when test="${empty username}">
                <h2>You are not logged in.</h2>
            </c:when>
            <c:otherwise>
                <form action="${pageContext.request.contextPath}/administration/logout" method="post">
                    <p>User <c:out value="${username}"/></p>
                    <input type="submit" value="Logout"/>
                </form>
                <br/>
                <h2>Files</h2>
                <form action="${pageContext.request.contextPath}/administration/process" method="post">
                    <table cellpadding="15" border="1" cellpadding="5">
                        <thead>
                            <tr>
                                <th>Filename</th>
                                <th>Uploaded</th>
                            </tr>
                        </thead>
                        <c:forEach items="${files}" var="file">
                            <tr>
                                <%
                                    UploadedFile file = (UploadedFile) pageContext.getAttribute("file");
                                %>
                                <td>                                    
                                    <%=file.getFilename()%>
                                </td>
                                <td>
                                    <%=file.getUploaded().toString()%>
                                </td>
                                <td>
                                    <input type="checkbox" name="selectedFiles" value="${file.getFilename()}"/>
                                </td>
                                <!--<td>
                                    <form method="post" action="${pageContext.request.contextPath}/administration/delete?filename=${file.getFilename()}">
                                        <input type="submit" value="Delete"/>
                                    </form>
                                </td>-->
                            </tr>
                        </c:forEach>
                    </table>
                    <br/>
                    <input type="submit" name="processFiles" value="Process selected"/>
                    <input type="submit" name="deleteFiles" value="Delete selected"/>
                </form>
                <br/><br/>
                <form method="post" action="${pageContext.request.contextPath}/administration/upload" enctype="multipart/form-data" >
                    File:
                    <input type="file" name="file"/>
                    <br/>
                    Source:
                    <select name="fileSource">
                        <option value="none">---</option>
                        <option value="eurostat">Eurostat</option>
                        <option value="cso-quartoen">ČSÚ - Štvrťročné dáta o platoch</option>
                        <option value="cso-cznace">ČSÚ - Ročné dáta o platoch (systém CZ-NACE)</option>
                        <option value="cso-reggend">ČSÚ - Ročné dáta o platoch (podľa pohlavia)</option>
                        <option value="cso-regkzam">ČSÚ - Ročné dáta o platoch (triedy KZAM)</option>
                    </select>
                    <br/>
                    <input type="submit" value="Upload"/>
                </form>
            </c:otherwise>
        </c:choose>
        <br/>
        <p>Links:</p>
        <a href="${pageContext.request.contextPath}/">Home</a>
        <br/>
        <a href="${pageContext.request.contextPath}/login">Login</a>
    </body>
</html>
