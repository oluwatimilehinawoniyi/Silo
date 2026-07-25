INSERT INTO ledger_accounts (id, code, name, type, active, created_at)
VALUES
    ('9d3f1a10-0001-4a3b-9c1e-1a2b3c4d5e01', '1000', 'Cash and Bank', 'ASSET', TRUE, NOW()),
    ('9d3f1a10-0001-4a3b-9c1e-1a2b3c4d5e02', '1100', 'Loans Receivable', 'ASSET', TRUE, NOW()),
    ('9d3f1a10-0001-4a3b-9c1e-1a2b3c4d5e03', '1200', 'Guarantor Receivable', 'ASSET', TRUE, NOW()),
    ('9d3f1a10-0001-4a3b-9c1e-1a2b3c4d5e04', '2000', 'Member Contributions Payable', 'LIABILITY', TRUE, NOW()),
    ('9d3f1a10-0001-4a3b-9c1e-1a2b3c4d5e05', '3000', 'Cooperative Fund Equity', 'EQUITY', TRUE, NOW()),
    ('9d3f1a10-0001-4a3b-9c1e-1a2b3c4d5e06', '4000', 'Interest Income', 'INCOME', TRUE, NOW()),
    ('9d3f1a10-0001-4a3b-9c1e-1a2b3c4d5e07', '4100', 'Penalty Income', 'INCOME', TRUE, NOW());
