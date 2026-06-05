# Database Design - Smart Banking Queue System

## 1. Purpose

This document defines the initial database design for Smart Banking Queue System (SBQS).

The database stores business data such as branches, counters, services, customers, appointments, tickets, queue logs, and ratings.

Authentication data such as username, password, and user roles will be managed by Keycloak.

## 2. Core Tables

### branches

Stores bank branch information.

| Column | Meaning |
|---|---|
| branch_id | Primary key |
| branch_code | Unique branch code |
| branch_name | Branch display name |
| address | Branch address |
| phone | Branch phone number |
| status | ACTIVE / INACTIVE |
| created_at | Created time |

### counters

Stores service counters in each branch.

| Column | Meaning |
|---|---|
| counter_id | Primary key |
| branch_id | Foreign key to branches |
| counter_code | Counter code |
| counter_name | Counter display name |
| status | ACTIVE / INACTIVE |

### services

Stores banking service types.

| Column | Meaning |
|---|---|
| service_id | Primary key |
| service_code | Unique service code |
| service_name | Service display name |
| description | Service description |
| estimated_time | Estimated processing time in minutes |

### customers

Stores customer business profile.

| Column | Meaning |
|---|---|
| customer_id | Primary key |
| keycloak_user_id | User ID from Keycloak |
| full_name | Customer full name |
| phone | Customer phone number |
| email | Customer email |
| citizen_id | Citizen ID / CCCD |
| vip_level | NORMAL / VIP |
| created_at | Created time |

### appointments

Stores booking appointments.

| Column | Meaning |
|---|---|
| appointment_id | Primary key |
| customer_id | Foreign key to customers |
| branch_id | Foreign key to branches |
| service_id | Foreign key to services |
| appointment_date | Booking date |
| appointment_time | Booking time |
| status | PENDING / CONFIRMED / CANCELLED / COMPLETED |
| created_at | Created time |

### tickets

Stores queue tickets.

| Column | Meaning |
|---|---|
| ticket_id | Primary key |
| ticket_no | Queue number, for example A001 |
| appointment_id | Foreign key to appointments |
| counter_id | Foreign key to counters |
| status | WAITING / CALLED / PROCESSING / COMPLETED / EXPIRED |
| issued_at | Ticket issued time |
| called_at | Ticket called time |
| completed_at | Ticket completed time |

### queue_logs

Stores ticket status history.

| Column | Meaning |
|---|---|
| log_id | Primary key |
| ticket_id | Foreign key to tickets |
| old_status | Previous ticket status |
| new_status | New ticket status |
| changed_by | User who changed the status |
| changed_at | Changed time |

### ratings

Stores customer feedback.

| Column | Meaning |
|---|---|
| rating_id | Primary key |
| ticket_id | Foreign key to tickets |
| score | Rating score from 1 to 5 |
| comment | Feedback comment |
| created_at | Created time |

## 3. Relationships

```text
branches 1 - N counters
branches 1 - N appointments
customers 1 - N appointments
services 1 - N appointments
appointments 1 - 1 tickets
tickets 1 - N queue_logs
tickets 1 - 1 ratings