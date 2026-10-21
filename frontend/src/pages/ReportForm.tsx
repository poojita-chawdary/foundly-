import { useState, useEffect, FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ShieldCheck, ArrowRight } from "lucide-react";
import { api, Item } from "../api";
import { useApp } from "../context";
import { ErrorBox, Loading, useLoad } from "../components/ui";
export function ReportForm({ edit = false }: { edit?: boolean }) {
  const { type, id } = useParams();
  const { catalog, user } = useApp();
  const {
    data: old,
    error: loadError,
    loading,
  } = useLoad<Item>(edit ? "/reports/" + id : "/users/me");
  const [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [preview, setPreview] = useState("");
  const navigate = useNavigate();
  useEffect(
    () => () => {
      if (preview) URL.revokeObjectURL(preview);
    },
    [preview],
  );
  if (edit && loading) return <Loading />;
  if (edit && loadError) return <ErrorBox error={loadError} />;
  const report = edit ? old : null;
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError("");
    const f = new FormData(e.currentTarget);
    const input = {
      type: report?.type ?? (type === "found" ? "FOUND" : "LOST"),
      title: f.get("title"),
      categoryId: Number(f.get("categoryId")),
      colour: f.get("colour"),
      description: f.get("description"),
      locationId: Number(f.get("locationId")),
      eventDate: f.get("eventDate"),
    };
    let saved: Item | null = null;
    try {
      saved = await api<Item>(edit ? "/reports/" + id : "/reports", {
        method: edit ? "PUT" : "POST",
        body: JSON.stringify(input),
      });
      const file = f.get("file") as File;
      if (file?.size) {
        const upload = new FormData();
        upload.set("file", file);
        await api("/reports/" + saved.id + "/image", {
          method: "POST",
          body: upload,
        });
      }
      navigate("/items/" + saved.id);
    } catch (e) {
      if (saved) {
        navigate("/items/" + saved.id + "?uploadError=1");
      } else setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="narrow wide section">
      <Link className="text-link" to="/browse">
        ← Back to noticeboard
      </Link>
      <div className="eyebrow">EVERY DETAIL HELPS</div>
      <h1 className="page-title">
        {edit
          ? "Update your report."
          : type === "found"
            ? "Found something? Make someone’s day."
            : "Lost something? Start here."}
      </h1>
      <p>
        Share the basics. Keep unique identifying details for the private claim.
      </p>
      <form className="panel form" onSubmit={submit}>
        <ErrorBox error={error} />
        <label>
          Item title
          <input
            name="title"
            required
            maxLength={120}
            defaultValue={report?.title}
            placeholder="e.g. Blue insulated water bottle"
          />
        </label>
        <div className="form-row">
          <label>
            Category
            <select
              name="categoryId"
              required
              defaultValue={report?.categoryId ?? ""}
            >
              <option value="" disabled>
                Choose a category
              </option>
              {catalog.categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Colour (optional)
            <input name="colour" maxLength={50} defaultValue={report?.colour} />
          </label>
        </div>
        <label>
          Description
          <textarea
            name="description"
            required
            minLength={10}
            maxLength={2000}
            rows={4}
            defaultValue={report?.description}
            placeholder="Describe the item without sharing ID numbers, contact information, or secret ownership details."
          />
        </label>
        <div className="form-row">
          <label>
            Campus location
            <select
              name="locationId"
              required
              defaultValue={report?.locationId ?? ""}
            >
              <option value="" disabled>
                Choose a general location
              </option>
              {catalog.locations
                .filter((c) => c.campusId === user?.campusId)
                .map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
            </select>
          </label>
          <label>
            Date{" "}
            {type === "found" || report?.type === "FOUND" ? "found" : "lost"}
            <input
              name="eventDate"
              type="date"
              required
              max={new Date().toLocaleDateString("en-CA")}
              defaultValue={report?.eventDate}
            />
          </label>
        </div>
        <label>
          Photo (optional)
          <input
            name="file"
            type="file"
            accept="image/jpeg,image/png"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file && file.size > 5 * 1024 * 1024) {
                e.target.value = "";
                setError("Choose an image smaller than 5 MB.");
                return;
              }
              setError("");
              setPreview(file ? URL.createObjectURL(file) : "");
            }}
          />
          <small>
            PNG or JPEG · up to 5 MB · 12 megapixels maximum. Cover personal
            details first.
          </small>
        </label>
        {preview && (
          <img
            className="upload-preview"
            alt="Selected image preview"
            src={preview}
          />
        )}
        <div className="notice">
          <ShieldCheck size={20} /> Don’t include phone numbers, ID numbers, or
          private verification details.
        </div>
        <button className="button" disabled={busy}>
          {busy ? "Saving your report…" : edit ? "Save changes" : "Post report"}
          <ArrowRight size={17} />
        </button>
      </form>
    </section>
  );
}
