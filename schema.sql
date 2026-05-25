-- PostgreSQL Database Schema for RentPay PG Manager
-- Suitable for execution directly in Supabase SQL Editor

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    email TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    is_authenticated BOOLEAN DEFAULT false,
    last_login_time BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- Enable Row Level Security (optional but default-enabled on new Supabase tables)
ALTER TABLE users DISABLE ROW LEVEL SECURITY;

-- 2. Tenants Table
CREATE TABLE IF NOT EXISTS tenants (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    gender TEXT NOT NULL,
    id_proof_type TEXT NOT NULL,
    id_proof_number TEXT NOT NULL,
    photo_path TEXT,
    room_number TEXT NOT NULL,
    bed_number TEXT NOT NULL,
    sharing_type TEXT NOT NULL,
    monthly_rent REAL NOT NULL,
    deposit_amount REAL NOT NULL,
    move_in_date TEXT NOT NULL,
    rent_due_date INTEGER NOT NULL,
    notes TEXT DEFAULT '',
    is_archived BOOLEAN DEFAULT false,
    sync_status TEXT DEFAULT 'SYNCED'
);

ALTER TABLE tenants DISABLE ROW LEVEL SECURITY;

-- 3. Rent Payments Table
CREATE TABLE IF NOT EXISTS rent_payments (
    id SERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    tenant_name TEXT NOT NULL,
    room_number TEXT NOT NULL,
    amount_paid REAL NOT NULL,
    payment_mode TEXT NOT NULL,
    month TEXT NOT NULL,
    payment_date BIGINT NOT NULL,
    notes TEXT DEFAULT '',
    sync_status TEXT DEFAULT 'SYNCED'
);

ALTER TABLE rent_payments DISABLE ROW LEVEL SECURITY;
