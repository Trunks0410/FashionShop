<header>
    <nav class="navbar navbar-expand-lg navbar-light bg-light">
        <a class="navbar-brand" href="/">OneShop</a>
        <div class="collapse navbar-collapse">
            <ul class="navbar-nav mr-auto">
                <li class="nav-item"><a class="nav-link" href="/home">Trang chủ</a></li>
                <li class="nav-item"><a class="nav-link" href="/search">Tìm kiếm</a></li>
                <security:authorize access="hasRole('ROLE_SHIPPER')">
                    <li class="nav-item"><a class="nav-link" href="/shipper/orders">Đơn hàng</a></li>
                </security:authorize>
            </ul>
            <ul class="navbar-nav">
                <security:authorize access="isAuthenticated()">
                    <li class="nav-item"><a class="nav-link" href="/logout">Đăng xuất</a></li>
                </security:authorize>
                <security:authorize access="!isAuthenticated()">
                    <li class="nav-item"><a class="nav-link" href="/login">Đăng nhập</a></li>
                    <li class="nav-item"><a class="nav-link" href="/register">Đăng ký</a></li>
                </security:authorize>
            </ul>
        </div>
    </nav>
</header>