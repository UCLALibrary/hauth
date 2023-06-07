--
-- PostgreSQL globals initialization
--

SET default_transaction_read_only = off;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

--
-- Roles
--

CREATE ROLE authz_reader;
ALTER ROLE authz_reader WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD '${test.db.password}';
CREATE ROLE authz_writer;
ALTER ROLE authz_writer WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD '${test.db.password}';

--
-- PostgreSQL database initialization
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;
SET default_tablespace = '';
SET default_table_access_method = heap;

--
-- Name: items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.items (
    uid text NOT NULL,
    access_mode smallint DEFAULT 0 NOT NULL
);

ALTER TABLE public.items OWNER TO postgres;

--
-- Name: COLUMN items.uid; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.items.uid IS 'The unique identifier of the requested object';

--
-- Name: COLUMN items.access_mode; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.items.access_mode IS 'The access mode of the requested item: 0 is open, 1 is restricted to campus network users';

--
-- Name: items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.items (uid, access_mode) FROM stdin;
\.

--
-- Name: items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items
    ADD CONSTRAINT items_pkey PRIMARY KEY (uid);

--
-- Name: TABLE items; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT ON TABLE public.items TO authz_reader;
GRANT ALL ON TABLE public.items TO authz_writer;
