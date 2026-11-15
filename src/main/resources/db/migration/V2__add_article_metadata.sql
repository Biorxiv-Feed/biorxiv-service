-- Store every field returned by the bioRxiv "details" endpoint.
-- Nullability: only columns that business logic depends on are NOT NULL
-- (category: subscriber matching; publication_date: fetch window). The API has no schema
-- contract, so optional metadata stays nullable rather than dropping an article.

ALTER TABLE articles
    ALTER COLUMN category SET NOT NULL,
    ALTER COLUMN publication_date SET NOT NULL,
    -- normalized lower case: new results / confirmatory results / contradictory results / withdrawn
    ADD COLUMN type                             VARCHAR(64),
    -- cc_by, cc_by_nc, cc_by_nc_nd, cc_by_nd, cc_no, cc0, cc0_ng
    ADD COLUMN license                          VARCHAR(64),
    -- journal DOI once the preprint is published; the API's "NA" is stored as NULL
    ADD COLUMN published_doi                    VARCHAR(255),
    ADD COLUMN corresponding_author             TEXT,
    ADD COLUMN corresponding_author_institution TEXT,
    ADD COLUMN server                           VARCHAR(32);
