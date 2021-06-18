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
ALTER ROLE authz_reader
  WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD '${pgsql.test.password}';

CREATE ROLE authz_writer;
ALTER ROLE authz_writer
  WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD '${pgsql.test.password}';

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
    item_id text NOT NULL,
    access_level smallint DEFAULT 0 NOT NULL,
    cookie_id int NOT NULL
);

CREATE TABLE public.cookies (
    cookie_id int GENERATED ALWAYS AS IDENTITY,
    cookie_name text NOT NULL
);

ALTER TABLE public.items OWNER TO postgres;
ALTER TABLE public.cookies OWNER TO postgres;

--
-- Name: COLUMN items.item_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.items.item_id IS 'The identifier of the requested item';

--
-- Name: COLUMN items.access_level; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.items.access_level IS 'The access level of the requested item: 0 is open access, 1 is restricted to UCLA affiliates, 2 is degraded access';

--
-- Name: COLUMN cookies.cookie_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.cookies.cookie_id IS 'The ID of a cookie';

--
-- Name: COLUMN cookies.cookie_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.cookies.cookie_name IS 'The name of a cookie';

--
-- Name: items items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items
    ADD CONSTRAINT items_pkey PRIMARY KEY (item_id);

--
-- Name: items cookies_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cookies
    ADD CONSTRAINT cookies_pkey PRIMARY KEY (cookie_id);

--
-- Name: items fk_cookie; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items
    ADD CONSTRAINT fk_cookie FOREIGN KEY (cookie_id) REFERENCES public.cookies (cookie_id);

--
-- Data for Name: cookies; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.cookies (cookie_name) FROM stdin;
acs_cookie
sinai_authenticated_3day
\.

--
-- Data for Name: items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.items (item_id, access_level, cookie_id) FROM stdin with delimiter '|';
ark:/21198/zz002dvwr6|0|1
ark:/21198/zz112dvwr7|2|1
ark:/21198/zz332dvwr8|1|2
\.

--
-- Name: TABLE items; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT ON TABLE public.items TO authz_reader;
GRANT ALL ON TABLE public.items TO authz_writer;

--
-- Name: TABLE cookies; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT ON TABLE public.cookies TO authz_reader;
GRANT ALL ON TABLE public.cookies TO authz_writer;
