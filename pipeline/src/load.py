# pipeline/src/load.py
# This script loads the clean, normalised transactions dataset into the PostgreSQL database.

import pandas as pd
import psycopg2
from psycopg2.extras import execute_values
import logging
import os
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


def get_connection():
    return psycopg2.connect(
        host=os.getenv('POSTGRES_HOST', 'localhost'),
        port=os.getenv('POSTGRES_PORT', '5432'),
        dbname=os.getenv('POSTGRES_DB', 'ownfinance'),
        user=os.getenv('POSTGRES_USER', 'ownfinance_user'),
        password=os.getenv('POSTGRES_PASSWORD', 'changeme')
    )


def get_or_create_demo_user(conn) -> str:
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM users WHERE email = %s", ('demo@ownfinance.ai',))
        row = cur.fetchone()
        if row:
            return str(row[0])

        cur.execute("""
            INSERT INTO users (email, password, full_name)
            VALUES (%s, %s, %s)
            RETURNING id
        """, ('demo@ownfinance.ai', 'hashed_placeholder', 'Demo User'))
        return str(cur.fetchone()[0])


def get_category_map(conn) -> dict:
    with conn.cursor() as cur:
        cur.execute("SELECT name, id FROM categories")
        return {name: cat_id for name, cat_id in cur.fetchall()}


def load(df: pd.DataFrame, conn) -> dict:
    metrics = {}
    user_id = get_or_create_demo_user(conn)
    category_map = get_category_map(conn)

    logger.info(f"Loading {len(df)} transactions for user {user_id}")

    rows = []
    skipped = 0
    for _, row in df.iterrows():
        category_id = category_map.get(row['category'])
        if category_id is None:
            logger.warning(f"Category not found: {row['category']} — using NULL")
            skipped += 1

        rows.append((
            user_id,
            category_id,
            row['description'],
            float(row['amount']),
            row['type'],
            row['transaction_date'].date() if hasattr(row['transaction_date'], 'date') else row['transaction_date'],
            'PIPELINE',
        ))

    with conn.cursor() as cur:
        # Cleans previous transactions from this pipeline run (from this user) to avoid duplicates on re-runs
        cur.execute("""
            DELETE FROM transactions
            WHERE user_id = %s AND source = 'PIPELINE'
        """, (user_id,))
        deleted = cur.rowcount
        logger.info(f"Previous transactions removed: {deleted}")

        # Upsert em batch
        execute_values(cur, """
            INSERT INTO transactions
                (user_id, category_id, description, amount, type, transaction_date, source)
            VALUES %s
        """, rows)

        inserted = len(rows)

    conn.commit()

    metrics['user_id'] = user_id
    metrics['inserted'] = inserted
    metrics['skipped_categories'] = skipped

    logger.info(f"Loading completed: {inserted} transactions inserted")
    return metrics


if __name__ == '__main__':
    df = pd.read_csv('data/clean_transactions.csv', parse_dates=['transaction_date'])

    conn = get_connection()
    try:
        metrics = load(df, conn)
        print(f"\nInserted transactions: {metrics['inserted']}")
        print(f"Demo user ID:    {metrics['user_id']}")
    finally:
        conn.close()