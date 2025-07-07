import React from 'react';
import { FaShoppingCart } from 'react-icons/fa';
import './ProductCard.css';

const ProductCard = ({ product }) => {
    return (
        <div className="product-card h-100">
            <img src={product.imageUrl} alt={product.name} />
            <h3 className="product-title">{product.name}</h3>
            <div className="price-container">
                <span className="original-price">฿{product.originalPrice.toLocaleString()}</span>
                <span className="discounted-price">฿{product.discountedPrice.toLocaleString()}</span>
            </div>
            <button className="add-to-cart-btn" aria-label={`Add ${product.name} to cart`}>
                <FaShoppingCart />
            </button>
        </div>
    );
};

export default ProductCard;