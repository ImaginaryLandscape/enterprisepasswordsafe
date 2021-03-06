<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
Copyright (c) 2017 Carbon Security Ltd. <opensource@carbonsecurity.co.uk>

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
  --%>

<c:set var="input_name" value="${param.input_name}" />
<c:set var="input_value"   value="${param.input_value}" />
<c:choose>
    <c:when test="${input_value == 'Y'}">
        <label class="radio-inline">
            <input type="radio" name="${input_name}" id="${input_name}_y" value="Y" checked="checked" /> Enabled
        </label>&nbsp;&nbsp;<label class="radio-inline">
            <input type="radio" name="${input_name}" id="${input_name}_n" value="N" /> Disabled
        </label>
    </c:when>
    <c:otherwise>
        <label class="radio-inline">
            <input type="radio" name="${input_name}" id="${input_name}_y" value="Y" /> Enabled
        </label>&nbsp;&nbsp;<label class="radio-inline">
            <input type="radio" name="${input_name}" id="${input_name}_n" value="N" checked="checked" /> Disabled
        </label>
    </c:otherwise>
</c:choose>
