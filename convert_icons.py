#!/usr/bin/env python3
"""
Script to convert Eva Icon SVG files to Android vector drawable format.
This script extracts path data from SVG files and converts them to Android XML format.
"""

import os
import xml.etree.ElementTree as ET
from pathlib import Path
import re

# Mapping of current icon names to Eva Icon names
ICON_MAPPING = {
    'ic_close': 'close',
    'ic_settings': 'settings',
    'ic_history': 'clock',  # Using clock icon for history
    'ic_search': 'search',
    'ic_delete': 'trash',
    'ic_catalog': 'grid',
    'ic_edit': 'edit',
    'ic_share': 'share',
    'ic_content_copy': 'copy',
    'ic_chevron_back': 'chevron-left',
    'ic_chevron_forward': 'chevron-right',
    'ic_plus': 'plus',
    'ic_minus': 'minus',
    'ic_arrow_back': 'arrow-back',
    'ic_bitcoin': 'credit-card',  # Using credit card as placeholder, we'll keep bitcoin custom
    'ic_balance_check': 'checkmark-circle',
    'ic_check': 'checkmark',
    'ic_checkmark_circle': 'checkmark-circle-2',
    'ic_home': 'home',
    'ic_scan': 'camera',  # Using camera for scan
    'ic_top_up': 'arrow-upward',  # Using upward arrow for top up
    'ic_switch_currency': 'swap',
    'ic_more_vert': 'more-vertical',
    'ic_open_with': 'share',  # Using share icon
    'ic_card': 'credit-card',
    'ic_light_mode': 'sun',  # Need to check if sun exists
    'ic_dark_mode': 'moon',  # Need to check if moon exists
    'ic_contactless': 'radio',  # Placeholder
    'ic_contactless_waves': 'activity',  # Placeholder
    'ic_circle_green': 'checkmark-circle',  # Using checkmark circle
    'ic_dollar_white': 'credit-card',  # Placeholder
    'ic_launcher_background': None,  # Skip launcher icons
    'ic_launcher_foreground': None,
    'ic_image_placeholder': 'image',  # Need to check
    'ic_profile_placeholder': 'person',  # Need to check
}

def svg_to_android_vector(svg_path, output_path, fill_color="#FF000000"):
    """
    Convert an SVG file to Android vector drawable format.
    
    Args:
        svg_path: Path to the SVG file
        output_path: Path where the Android vector drawable should be saved
        fill_color: Color to use for the fill (default black)
    """
    try:
        tree = ET.parse(svg_path)
        root = tree.getroot()
        
        # Get viewBox from SVG
        viewbox = root.get('viewBox', '0 0 24 24')
        viewbox_parts = viewbox.split()
        width = viewbox_parts[2] if len(viewbox_parts) > 2 else '24'
        height = viewbox_parts[3] if len(viewbox_parts) > 3 else '24'
        
        # Find all paths and circles in the SVG
        svg_ns = '{http://www.w3.org/2000/svg}'
        paths = root.findall(f'.//{svg_ns}path') or root.findall('.//path')
        circles = root.findall(f'.//{svg_ns}circle') or root.findall('.//circle')
        
        if not paths and not circles:
            print(f"Warning: No paths or circles found in {svg_path}")
            return False
        
        # Build Android vector drawable XML
        android_ns = 'http://schemas.android.com/apk/res/android'
        
        vector = ET.Element('vector')
        vector.set('xmlns:android', android_ns)
        vector.set('android:width', '24dp')
        vector.set('android:height', '24dp')
        vector.set('android:viewportWidth', width)
        vector.set('android:viewportHeight', height)
        
        # Add all paths
        for path in paths:
            path_data = path.get('d', '')
            if path_data:
                android_path = ET.SubElement(vector, 'path')
                android_path.set('android:fillColor', fill_color)
                android_path.set('android:pathData', path_data)
        
        # Convert circles to paths
        for circle in circles:
            cx = float(circle.get('cx', 12))
            cy = float(circle.get('cy', 12))
            r = float(circle.get('r', 2))
            # Convert circle to path using arc commands
            # M cx-r,cy A r,r 0 1,1 cx+r,cy A r,r 0 1,1 cx-r,cy
            path_data = f"M {cx-r},{cy} A {r},{r} 0 1,1 {cx+r},{cy} A {r},{r} 0 1,1 {cx-r},{cy}"
            android_path = ET.SubElement(vector, 'path')
            android_path.set('android:fillColor', fill_color)
            android_path.set('android:pathData', path_data)
        
        # Write to file
        tree = ET.ElementTree(vector)
        ET.indent(tree, space='    ')
        
        # Write XML declaration and content
        with open(output_path, 'wb') as f:
            f.write(b'<?xml version="1.0" encoding="utf-8"?>\n')
            tree.write(f, encoding='utf-8')
        
        return True
        
    except Exception as e:
        print(f"Error converting {svg_path}: {e}")
        return False

def main():
    """Main function to convert all icons."""
    base_dir = Path(__file__).parent
    eva_icons_dir = base_dir / 'node_modules' / 'eva-icons' / 'fill' / 'svg'
    android_drawable_dir = base_dir / 'app' / 'src' / 'main' / 'res' / 'drawable'
    
    if not eva_icons_dir.exists():
        print(f"Error: Eva icons directory not found at {eva_icons_dir}")
        return
    
    if not android_drawable_dir.exists():
        print(f"Error: Android drawable directory not found at {android_drawable_dir}")
        return
    
    converted = 0
    skipped = 0
    failed = 0
    
    # Convert each icon
    for android_icon_name, eva_icon_name in ICON_MAPPING.items():
        if eva_icon_name is None:
            print(f"Skipping {android_icon_name} (no mapping)")
            skipped += 1
            continue
        
        eva_icon_path = eva_icons_dir / f"{eva_icon_name}.svg"
        android_icon_path = android_drawable_dir / f"{android_icon_name}.xml"
        
        if not eva_icon_path.exists():
            print(f"Warning: Eva icon not found: {eva_icon_path}")
            failed += 1
            continue
        
        # Check if original Android icon exists to preserve tint/color attributes
        original_color = "#FF000000"  # Default black
        if android_icon_path.exists():
            try:
                tree = ET.parse(android_icon_path)
                root = tree.getroot()
                # Check for tint attribute
                tint = root.get('{http://schemas.android.com/apk/res/android}tint')
                if tint:
                    # If has tint, use transparent fill so tint can work
                    original_color = "@android:color/white"
                else:
                    # Try to get fillColor from first path
                    path = root.find('.//path')
                    if path is not None:
                        fill = path.get('{http://schemas.android.com/apk/res/android}fillColor')
                        if fill:
                            original_color = fill
            except:
                pass
        
        if svg_to_android_vector(eva_icon_path, android_icon_path, original_color):
            print(f"Converted: {android_icon_name} <- {eva_icon_name}.svg")
            converted += 1
        else:
            failed += 1
    
    print(f"\nConversion complete!")
    print(f"Converted: {converted}")
    print(f"Skipped: {skipped}")
    print(f"Failed: {failed}")

if __name__ == '__main__':
    main()
