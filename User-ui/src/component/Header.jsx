import React from 'react';
import { Container, Navbar, Nav, Form, InputGroup, Button } from 'react-bootstrap';
import { FaSearch, FaUser, FaUserPlus } from 'react-icons/fa';
import './Header.css';

const Header = () => {
    return (
        <Navbar bg="white" expand="lg" className="border-bottom shadow-sm py-3 sticky-top">
            <Container>
                <Navbar.Brand href="#home" className="fw-bold logo-color">IT SHOP</Navbar.Brand>
                
                <Navbar.Toggle aria-controls="basic-navbar-nav" />

                <Navbar.Collapse id="basic-navbar-nav">
                    <Nav className="me-auto">
                        <Nav.Link href="#spec">จัดสเปคคอม</Nav.Link>
                        <Nav.Link href="#category">หมวดหมู่สินค้า ▼</Nav.Link>
                    </Nav>

                    <Form className="d-flex my-2 my-lg-0 mx-auto" style={{ maxWidth: '400px', width: '100%' }}>
                        <InputGroup>
                            <Form.Control
                                type="search"
                                placeholder="ค้นหาสินค้า"
                                aria-label="Search"
                            />
                            <Button variant="danger" id="button-search">
                                <FaSearch />
                            </Button>
                        </InputGroup>
                    </Form>

                    <Nav className="ms-lg-auto">
                        <Nav.Link href="#login" className="d-flex align-items-center">
                            <FaUser className="me-2" /> เข้าสู่ระบบ
                        </Nav.Link>
                        <Nav.Link href="#register" className="d-flex align-items-center">
                            <FaUserPlus className="me-2" /> สมัครสมาชิก
                        </Nav.Link>
                    </Nav>
                </Navbar.Collapse>
            </Container>
        </Navbar>
    );
};

export default Header;