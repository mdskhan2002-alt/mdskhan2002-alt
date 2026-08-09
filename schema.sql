-- ============================================================================
-- COCOAURA FOODS ERP & CRM - COMPREHENSIVE POSTGRESQL DDL SCHEMA
-- ============================================================================
-- This script establishes the highly normalized relational database schema
-- for the CocoAura Foods ecosystem.
-- 
-- Key Highlights:
-- 1. Custom User Roles Enum (admin, customer, distributor, retailer)
-- 2. Core 'users' table using UUIDs and unique constraints on email and phone
-- 3. Normalized Linked Entity Tables (customers, distributors, retailers)
--    secured with strict foreign key constraints (ON DELETE CASCADE)
-- 4. Transactional & Supply Chain tables (products, inventory, orders, order_items)
-- 5. CRM & Loyalty tables (crm_tickets, loyalty_transactions)
-- 6. Dynamic update triggers for auto-updating timestamps
-- 7. High-performance index declarations
-- 8. Realistic, comprehensive ERP and CRM initialization seed data
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. DATABASE CLEANUP & EXTENSIONS
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Drop tables in reverse-dependency order to clean any existing state cleanly
DROP TABLE IF EXISTS loyalty_transactions CASCADE;
DROP TABLE IF EXISTS crm_tickets CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS inventory CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS retailers CASCADE;
DROP TABLE IF EXISTS distributors CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Drop custom types
DROP TYPE IF EXISTS user_role_enum CASCADE;
DROP TYPE IF EXISTS kyc_status_enum CASCADE;
DROP TYPE IF EXISTS order_status_enum CASCADE;
DROP TYPE IF EXISTS payment_status_enum CASCADE;
DROP TYPE IF EXISTS ticket_status_enum CASCADE;

-- ----------------------------------------------------------------------------
-- 2. ENUM TYPE DEFINITIONS
-- ----------------------------------------------------------------------------
CREATE TYPE user_role_enum AS ENUM ('admin', 'customer', 'distributor', 'retailer');
CREATE TYPE kyc_status_enum AS ENUM ('Pending', 'Approved', 'Rejected');
CREATE TYPE order_status_enum AS ENUM ('Placed', 'Processing', 'Shipped', 'Delivered', 'Cancelled');
CREATE TYPE payment_status_enum AS ENUM ('Pending', 'Success', 'Failed', 'On Credit');
CREATE TYPE ticket_status_enum AS ENUM ('Open', 'In Progress', 'Resolved');

-- ----------------------------------------------------------------------------
-- 3. CORE TABLE DEFINITIONS WITH FOREIGN KEYS
-- ----------------------------------------------------------------------------

-- A. USERS TABLE
-- Primary identity ledger containing credential and role descriptors
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL, -- Bcrypt hash of credentials
    full_name VARCHAR(100) NOT NULL,
    role user_role_enum NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- B. CUSTOMERS PROFILE (Linked Entity)
-- Holds consumer-focused CRM fields such as loyalty tracking
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    loyalty_points INTEGER DEFAULT 0 NOT NULL CHECK (loyalty_points >= 0),
    tier VARCHAR(20) DEFAULT 'Bronze' NOT NULL CHECK (tier IN ('Bronze', 'Silver', 'Gold', 'Platinum')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- C. DISTRIBUTORS PROFILE (Linked Entity)
-- Holds bulk partner supply metrics, credit line authorizations, and territory specs
CREATE TABLE distributors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(150) NOT NULL,
    region VARCHAR(100) NOT NULL,
    credit_limit DECIMAL(12, 2) DEFAULT 0.00 NOT NULL CHECK (credit_limit >= 0.00),
    credit_used DECIMAL(12, 2) DEFAULT 0.00 NOT NULL CHECK (credit_used >= 0.00),
    kyc_status kyc_status_enum DEFAULT 'Pending' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT check_credit_balance CHECK (credit_used <= credit_limit)
);

-- D. RETAILERS PROFILE (Linked Entity)
-- Holds regional physical store contexts and localized credit allowances
CREATE TABLE retailers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_name VARCHAR(150) NOT NULL,
    region VARCHAR(100) NOT NULL,
    credit_limit DECIMAL(12, 2) DEFAULT 0.00 NOT NULL CHECK (credit_limit >= 0.00),
    credit_used DECIMAL(12, 2) DEFAULT 0.00 NOT NULL CHECK (credit_used >= 0.00),
    kyc_status kyc_status_enum DEFAULT 'Pending' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT check_retailer_credit CHECK (credit_used <= credit_limit)
);

-- E. PRODUCTS TABLE
-- Maps product stock keeping units (SKUs) to nutrient profiles and retail/wholesale cost rates
CREATE TABLE products (
    sku VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    nutrition_json JSONB, -- Nutrient panel metadata
    retail_price DECIMAL(10, 2) NOT NULL CHECK (retail_price >= 0.00),
    distributor_price DECIMAL(10, 2) NOT NULL CHECK (distributor_price >= 0.00),
    is_available BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- F. INVENTORY TABLE
-- Distributes stock allocations across decentralized regional supply warehouses
CREATE TABLE inventory (
    id SERIAL PRIMARY KEY,
    warehouse_name VARCHAR(100) NOT NULL,
    product_sku VARCHAR(50) NOT NULL REFERENCES products(sku) ON DELETE CASCADE,
    qty INTEGER DEFAULT 0 NOT NULL CHECK (qty >= 0),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_warehouse_product UNIQUE (warehouse_name, product_sku)
);

-- G. ORDERS TABLE
-- High-integrity billing logs linking orders directly to physical users
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL, -- Prevent order loss if user deletes account
    buyer_type VARCHAR(50) NOT NULL CHECK (buyer_type IN ('D2C_Customer', 'Distributor', 'Retailer')),
    buyer_name VARCHAR(150) NOT NULL, -- Denormalized for invoice snapshot historical accuracy
    status order_status_enum DEFAULT 'Placed' NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL CHECK (total_amount >= 0.00),
    payment_status payment_status_enum DEFAULT 'Pending' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- H. ORDER_ITEMS TABLE
-- Holds atomic lines items inside individual transactional cycles
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_sku VARCHAR(50) NOT NULL REFERENCES products(sku) ON DELETE RESTRICT,
    product_name VARCHAR(150) NOT NULL, -- Denormalized for receipt records
    qty INTEGER NOT NULL CHECK (qty > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0.00),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- I. CRM TICKETS TABLE
-- Support ticket journal for logging quality assurance & supply chain queries
CREATE TABLE crm_tickets (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    buyer_name VARCHAR(150) NOT NULL,
    buyer_type VARCHAR(50) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    notes TEXT NOT NULL,
    status ticket_status_enum DEFAULT 'Open' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- J. LOYALTY TRANSACTIONS TABLE
-- Ledger tracks real-time points accruals, QR scan payouts, and shop redemptions
CREATE TABLE loyalty_transactions (
    id SERIAL PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    points INTEGER NOT NULL, -- Positive for accumulation, negative for redemption
    type VARCHAR(50) NOT NULL CHECK (type IN ('Earned_QR', 'Redeemed_Item', 'Purchase_Reward')),
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);


-- ----------------------------------------------------------------------------
-- 4. DATABASE TRIGGERS FOR METADATA SYNCHRONIZATION
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_timestamp_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply timestamp triggers
CREATE TRIGGER trigger_update_users_timestamp BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
CREATE TRIGGER trigger_update_customers_timestamp BEFORE UPDATE ON customers FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
CREATE TRIGGER trigger_update_distributors_timestamp BEFORE UPDATE ON distributors FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
CREATE TRIGGER trigger_update_retailers_timestamp BEFORE UPDATE ON retailers FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
CREATE TRIGGER trigger_update_products_timestamp BEFORE UPDATE ON products FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
CREATE TRIGGER trigger_update_orders_timestamp BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();
CREATE TRIGGER trigger_update_crm_tickets_timestamp BEFORE UPDATE ON crm_tickets FOR EACH ROW EXECUTE FUNCTION update_timestamp_column();


-- ----------------------------------------------------------------------------
-- 5. PERFORMANCE OPTIMIZATION INDEXES
-- ----------------------------------------------------------------------------
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_customers_user_id ON customers(user_id);
CREATE INDEX idx_distributors_user_id ON distributors(user_id);
CREATE INDEX idx_retailers_user_id ON retailers(user_id);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_crm_tickets_user_id ON crm_tickets(user_id);
CREATE INDEX idx_loyalty_transactions_customer_id ON loyalty_transactions(customer_id);


-- ----------------------------------------------------------------------------
-- 6. ERP & CRM RICH SEED DATA
-- ----------------------------------------------------------------------------

-- A. SEED COCOAURA USERS (Using deterministic UUID keys)
INSERT INTO users (id, email, phone, password_hash, full_name, role) VALUES
('6a427f71-61b4-4b47-be8e-17cf7b949cf0', 'rahul.sharma@email.com', '+919876543210', '$2a$12$L7pYj4u3r...MOCK_HASH_1', 'Rahul Sharma', 'customer'),
('8b538e82-72c5-5c58-cf9f-28df8ca5acf1', 'ananya.roy@email.com', '+919911223344', '$2a$12$D3fXj8v7q...MOCK_HASH_2', 'Ananya Roy', 'customer'),
('9c649f93-83d6-6d69-da0a-39ef9db6bdf2', 'b2b.apex@apexdistributors.in', '+914842555666', '$2a$12$B9sRk1l4m...MOCK_HASH_3', 'Apex Distributors Ltd', 'distributor'),
('cd86be05-a5f8-8f8b-fc2c-5b0fbe38cdf4', 'pooja.kirana@retail.in', '+919008007006', '$2a$12$R5qVp2p9l...MOCK_HASH_4', 'Pooja Kirana Store (Bangalore)', 'retailer'),
('edf7cf06-b6f9-9f9c-0d3d-6c1fcf49cdf5', 'admin@cocoaurafoods.com', '+912245551122', '$2a$12$A1bC2dE3f...MOCK_HASH_5', 'Ecosystem Admin Operator', 'admin');

-- B. SEED LINKED PROFILE STATE DATA
-- Customer Loyalty Profiles
INSERT INTO customers (id, user_id, loyalty_points, tier) VALUES
('b3917838-51f4-4a27-a00d-ebf0e0f3400a', '6a427f71-61b4-4b47-be8e-17cf7b949cf0', 215, 'Gold'),
('c4828949-62f5-5b38-b11e-fce1f1f4511b', '8b538e82-72c5-5c58-cf9f-28df8ca5acf1', 150, 'Silver');

-- Distributor Bulk Partner Profile
INSERT INTO distributors (id, user_id, company_name, region, credit_limit, credit_used, kyc_status) VALUES
('d5739050-73f6-6c49-c22f-0df20205622c', '9c649f93-83d6-6d69-da0a-39ef9db6bdf2', 'Apex Distributors Ltd', 'Kerala Region', 500000.00, 125000.00, 'Approved');

-- Retailer Store Partner Profile
INSERT INTO retailers (id, user_id, store_name, region, credit_limit, credit_used, kyc_status) VALUES
('e6840161-84a7-7d5a-d33a-1ef31316733d', 'cd86be05-a5f8-8f8b-fc2c-5b0fbe38cdf4', 'Pooja Kirana Store', 'South Bangalore', 50000.00, 0.00, 'Approved');

-- C. SEED PRODUCT INVENTORY
INSERT INTO products (sku, name, category, description, nutrition_json, retail_price, distributor_price, is_available) VALUES
('COCO-WATER-01', 'Pure Tender Coconut Water', 'Beverages', '100% natural, electrolyte-rich tender coconut water harvested fresh from organic coastal groves. Fat-free with no added sugars or preservatives.', '{"Calories": "45 kcal", "Potassium": "600mg", "Natural Sugars": "9g", "Sodium": "40mg"}', 50.00, 35.00, TRUE),
('COCO-OIL-02', 'Organic Virgin Coconut Oil', 'Wellness', 'Premium cold-pressed, unrefined virgin coconut oil. Rich in medium-chain triglycerides (MCTs) and Lauric acid. Ideal for cooking, hair, and skin wellness.', '{"Lauric Acid": "49%", "MCTs": "62%", "Total Fat": "14g", "Trans Fat": "0g"}', 350.00, 240.00, TRUE),
('COCO-CHIPS-03', 'Baked Crunchy Coconut Chips', 'Snacks', 'Toasted, slow-baked organic coconut flakes seasoned with a delicate pinch of natural sea salt. Gluten-free, high fiber, crunchy snack perfection.', '{"Calories": "160 kcal", "Dietary Fiber": "4g", "Carbs": "8g", "Total Fat": "12g"}', 80.00, 55.00, TRUE),
('COCO-BITES-04', 'Roasted Coconut Bites', 'Snacks', 'Delectable roasted coconut crunch bites glazed with a thin coat of premium organic dark chocolate. High-antioxidant sweet wellness treats.', '{"Calories": "140 kcal", "Sugar": "6g", "Iron": "1.2mg", "Total Fat": "10g"}', 120.00, 85.00, TRUE);

-- D. SEED DECENTRALIZED SUPPLY STOCK
INSERT INTO inventory (warehouse_name, product_sku, qty) VALUES
('Coastal Hub (Kochi)', 'COCO-WATER-01', 5000),
('Coastal Hub (Kochi)', 'COCO-OIL-02', 1200),
('Central Hub (Bangalore)', 'COCO-CHIPS-03', 3500),
('Central Hub (Bangalore)', 'COCO-BITES-04', 2000);

-- E. SEED COMPLETED HISTORICAL ORDER RECORDS
INSERT INTO orders (id, user_id, buyer_type, buyer_name, status, total_amount, payment_status, created_at) VALUES
(1001, '6a427f71-61b4-4b47-be8e-17cf7b949cf0', 'D2C_Customer', 'Rahul Sharma', 'Delivered', 180.00, 'Success', NOW() - INTERVAL '10 days'),
(1002, '9c649f93-83d6-6d69-da0a-39ef9db6bdf2', 'Distributor', 'Apex Distributors Ltd', 'Shipped', 17500.00, 'On Credit', NOW() - INTERVAL '5 days'),
(1003, 'cd86be05-a5f8-8f8b-fc2c-5b0fbe38cdf4', 'Retailer', 'Pooja Kirana Store (Bangalore)', 'Processing', 4950.00, 'Success', NOW() - INTERVAL '1 day');

-- F. SEED DETAILED ITEM LINES FOR TRANSACTIONS
INSERT INTO order_items (order_id, product_sku, product_name, qty, unit_price) VALUES
(1001, 'COCO-WATER-01', 'Pure Tender Coconut Water', 2, 50.00),
(1001, 'COCO-CHIPS-03', 'Baked Crunchy Coconut Chips', 1, 80.00),
(1002, 'COCO-WATER-01', 'Pure Tender Coconut Water', 500, 35.00),
(1003, 'COCO-CHIPS-03', 'Baked Crunchy Coconut Chips', 50, 55.00),
(1003, 'COCO-BITES-04', 'Roasted Coconut Bites', 25, 85.00);

-- G. SEED LOYALTY TRANSACTIONS HISTORICAL LEDGER
INSERT INTO loyalty_transactions (customer_id, points, type, description, created_at) VALUES
('b3917838-51f4-4a27-a00d-ebf0e0f3400a', 15, 'Earned_QR', 'Scanned Pure Tender Coconut Water Batch AURA-WATER-BATCH42', NOW() - INTERVAL '10 days'),
('b3917838-51f4-4a27-a00d-ebf0e0f3400a', 200, 'Purchase_Reward', 'Earned via checkout order #1001', NOW() - INTERVAL '10 days'),
('c4828949-62f5-5b38-b11e-fce1f1f4511b', 150, 'Purchase_Reward', 'Earned welcome signup bonus points', NOW() - INTERVAL '15 days');

-- H. SEED CRM DEPLOYMENT TICKETS
INSERT INTO crm_tickets (user_id, buyer_name, buyer_type, subject, notes, status) VALUES
('6a427f71-61b4-4b47-be8e-17cf7b949cf0', 'Rahul Sharma', 'D2C_Customer', 'Delivery delays', 'My order #1001 was delayed by 2 days in transit. Purity is great, but logistics needs improvement.', 'Resolved'),
('cd86be05-a5f8-8f8b-fc2c-5b0fbe38cdf4', 'Pooja Kirana Store (Bangalore)', 'Retailer', 'Credit limit expansion', 'Requesting our credit line expansion to ₹100,000 for upcoming festive stock demand.', 'Open');

-- Reset sequences for key autoincrement consistency
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));
SELECT setval('order_items_id_seq', (SELECT MAX(id) FROM order_items));
SELECT setval('inventory_id_seq', (SELECT MAX(id) FROM inventory));
SELECT setval('crm_tickets_id_seq', (SELECT MAX(id) FROM crm_tickets));
SELECT setval('loyalty_transactions_id_seq', (SELECT MAX(id) FROM loyalty_transactions));
