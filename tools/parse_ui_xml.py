import sys
import xml.etree.ElementTree as ET

def main():
    path = sys.argv[1] if len(sys.argv) > 1 else "diag_local.xml"
    tree = ET.parse(path)
    for elem in tree.iter("node"):
        text = elem.attrib.get("text")
        desc = elem.attrib.get("content-desc")
        bounds = elem.attrib.get("bounds")
        clickable = elem.attrib.get("clickable") == "true"
        if text or desc:
            label = text if text else f"[Desc: {desc}]"
            click_str = " (Clickable)" if clickable else ""
            print(f"{label:<50} {bounds}{click_str}")

if __name__ == "__main__":
    main()

