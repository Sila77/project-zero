import React, { useState } from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import Header from './component/Header.jsx'; // <-- Corrected path and extension
import ProductCard from './component/ProductCard.jsx'; // <-- Corrected path and extension
import './App.css';

// Mock product data
const initialProducts = [
  { id: 1, name: 'INTEL CORE I9-14900K 3.2GHz 24C/32T', imageUrl: 'https://placehold.co/250x250/f04e23/white?text=CPU', originalPrice: 25900, discountedPrice: 23500 },
  { id: 2, name: 'ASUS ROG STRIX Z790-F GAMING WIFI II', imageUrl: 'https://placehold.co/250x250/333/white?text=Mainboard', originalPrice: 17890, discountedPrice: 16500 },
  { id: 3, name: 'GIGABYTE GEFORCE RTX 4090 GAMING OC - 24GB GDDR6X', imageUrl: 'https://placehold.co/250x250/76b900/white?text=GPU', originalPrice: 81900, discountedPrice: 79900 },
  { id: 4, name: 'SAMSUNG 990 PRO 2TB M.2 NVME/PCIE 4.0', imageUrl: 'https://placehold.co/250x250/0073e6/white?text=SSD', originalPrice: 8990, discountedPrice: 7590 },
  { id: 5, name: 'AMD RYZEN 9 7950X3D 4.2GHz 16C/32T', imageUrl: 'https://placehold.co/250x250/f04e23/white?text=CPU', originalPrice: 24500, discountedPrice: 22900 },
  { id: 6, name: 'ASROCK X670E TAICHI CARRARA', imageUrl: 'https://placehold.co/250x250/333/white?text=Mainboard', originalPrice: 22900, discountedPrice: 21590 },
  { id: 7, name: 'ASUS RADEON RX 7900 XTX TUF GAMING OC - 24GB GDDR6', imageUrl: 'https://placehold.co/250x250/ed1c24/white?text=GPU', originalPrice: 42900, discountedPrice: 39990 },
  { id: 8, name: 'CORSAIR MP600 PRO LPX 4TB M.2 NVME/PCIE 4.0', imageUrl: 'https://placehold.co/250x250/0073e6/white?text=SSD', originalPrice: 12900, discountedPrice: 11500 },
];


function App() {
    const [products, setProducts] = useState(initialProducts);
    const [activeCategory, setActiveCategory] = useState('CPU');

    return (
        <div className="app-container">
            <Header />
            <Container className="main-content">
                <Row className="filter-bar">
                    <Col>
                        <h2>Crafted with excellent material</h2>
                        <div className="category-filters">
                            <button onClick={() => setActiveCategory('CPU')} className={activeCategory === 'CPU' ? 'active' : ''}>CPU</button>
                            <button onClick={() => setActiveCategory('Mainboard')} className={activeCategory === 'Mainboard' ? 'active' : ''}>Mainboard</button>
                            <button onClick={() => setActiveCategory('GPU')} className={activeCategory === 'GPU' ? 'active' : ''}>GPU</button>
                            <button onClick={() => setActiveCategory('Harddisk')} className={activeCategory === 'Harddisk' ? 'active' : ''}>Harddisk</button>
                        </div>
                    </Col>
                </Row>

                <Row xs={1} sm={2} md={3} lg={4} className="g-4">
                    {products.map(product => (
                        <Col key={product.id}>
                            <ProductCard product={product} />
                        </Col>
                    ))}
                </Row>
            </Container>
            <footer className="text-center p-4 mt-auto" style={{backgroundColor: '#f8f9fa'}}>
                Copyright © 2024 IT SHOP. All Rights Reserved.
            </footer>
        </div>
    );
}

export default App;