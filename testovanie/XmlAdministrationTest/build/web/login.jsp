<%-- 
    Document   : login
    Created on : Jun 8, 2015, 7:15:16 PM
    Author     : Marek Jonis
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Login</title>
    </head>
    <body>
        <h1>Login page!</h1>
        <c:choose>
            <c:when test="${empty username}">
                <form action="${pageContext.request.contextPath}/login" method="post">  
                    <p>Username:</p>
                    <input type="text" name="username"/>
                    <br/>  
                    <p>Password:</p>
                    <input type="password" name="password"/>
                    <br/><br/>
                    <input type="submit" value="Login"/>  
                </form>
            </c:when>
            <c:otherwise>
                <h3>You are logged in as <c:out value="${username}"/>.</h3>
            </c:otherwise>            
        </c:choose>
        <c:if test="${not empty message}">
            <br/>
            <font style="color: red"><b>
                <c:out value="${message}"/>
            </b></font>
            <br/>
        </c:if>
        <p>Links:</p>
        <a href="${pageContext.request.contextPath}/administration">Administration</a>
        <br/>
        <a href="${pageContext.request.contextPath}/">Home</a>
    </body>
</html>
