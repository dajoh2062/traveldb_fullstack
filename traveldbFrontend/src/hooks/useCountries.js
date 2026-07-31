import { useEffect, useState } from "react";
import { fetchCountries } from "../api/travelApi";

export default function useCountries() {
  const [countries, setCountries] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [requestKey, setRequestKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    fetchCountries({ signal: controller.signal })
      .then(setCountries)
      .catch(() => {
        if (!controller.signal.aborted) {
          setCountries([]);
          setError("Country options could not be loaded.");
        }
      })
      .finally(() => { if (!controller.signal.aborted) setIsLoading(false); });
    return () => controller.abort();
  }, [requestKey]);

  function retry() {
    setCountries([]);
    setError("");
    setIsLoading(true);
    setRequestKey(value => value + 1);
  }

  return { countries, error, isLoading, retry };
}
