<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.sitemesh.org/sitemesh/decorator" prefix="sitemesh" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>OneShop Cosmetics - <sitemesh:write property='title'/></title>
    
    <sitemesh:write property='head'/>
</head>
<body>

   <div>
   <c:import url="/fragments/guest/header" />
   </div>
   

    <main>
        <sitemesh:write property='body'/>
    </main>

    <c:import url="/fragments/footer" />

</body>
</html>