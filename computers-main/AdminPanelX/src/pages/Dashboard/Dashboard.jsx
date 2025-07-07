// src/pages/Dashboard/Dashboard.jsx

import React from 'react';
import { Row, Col } from 'react-bootstrap';
import { BsFillArchiveFill, BsFillGrid3X3GapFill, BsPeopleFill, BsFillBellFill } from 'react-icons/bs';

import MainHeader from '../../components/MainHeader/MainHeader';
import PageHeader from '../../components/PageHeader/PageHeader';
import './Dashboard.css'; // <-- IMPORT THE NEW CSS FILE

function Dashboard() {
  return (
    <>
      <MainHeader />
      <PageHeader title="DASHBOARD" subtitle="Welcome to your dashboard" />

      {/* STATS CARDS */}
      <Row>
        <Col xs={12} sm={6} lg={3} className="mb-4">
          <div className="stat-card">
            <div className="stat-card__content">
              <div>
                <div className="stat-card__title">PRODUCTS</div>
                <div className="stat-card__value">300</div>
              </div>
              <BsFillArchiveFill className="stat-card__icon icon-products" />
            </div>
          </div>
        </Col>
        <Col xs={12} sm={6} lg={3} className="mb-4">
          <div className="stat-card">
            <div className="stat-card__content">
              <div>
                <div className="stat-card__title">CATEGORIES</div>
                <div className="stat-card__value">12</div>
              </div>
              <BsFillGrid3X3GapFill className="stat-card__icon icon-categories" />
            </div>
          </div>
        </Col>
        <Col xs={12} sm={6} lg={3} className="mb-4">
          <div className="stat-card">
            <div className="stat-card__content">
              <div>
                <div className="stat-card__title">CUSTOMERS</div>
                <div className="stat-card__value">33</div>
              </div>
              <BsPeopleFill className="stat-card__icon icon-customers" />
            </div>
          </div>
        </Col>
        <Col xs={12} sm={6} lg={3} className="mb-4">
          <div className="stat-card">
            <div className="stat-card__content">
              <div>
                <div className="stat-card__title">ALERTS</div>
                <div className="stat-card__value">42</div>
              </div>
              <BsFillBellFill className="stat-card__icon icon-alerts" />
            </div>
          </div>
        </Col>
      </Row>

      {/* CHART PLACEHOLDERS */}
      <Row>
        <Col xs={12} lg={8} className="mb-4">
          <div className="chart-card">
            <h4>Line Chart Placeholder</h4>
            <p>You can add a real chart component here later.</p>
          </div>
        </Col>
        <Col xs={12} lg={4} className="mb-4">
          <div className="chart-card">
            <h4>Revenue Circle</h4>
            <p>Placeholder for a progress circle.</p>
          </div>
        </Col>
      </Row>
    </>
  );
}

export default Dashboard;